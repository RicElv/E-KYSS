package ekyss.model;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DatabaseHandler {
    Database db;
    Connection conn;
    private final String[] TIMEREPORTCOLUMNS = {"11_d", "11_i", "11_f", "11_r", "11_t",
            "12_d", "12_i", "12_f", "12_r", "12_t",
            "13_d", "13_i", "13_f", "13_r", "13_t",
            "14_d", "14_i", "14_f", "14_r", "14_t",
            "15_d", "15_i", "15_f", "15_r", "15_t",
            "16_d", "16_i", "16_f", "16_r", "16_t",
            "17_d", "17_i", "17_f", "17_r", "17_t",
            "18_d", "18_i", "18_f", "18_r", "18_t",
            "19_d", "19_i", "19_f", "19_r", "19_t",
            "21_t", "22_t", "23_t", "30_t", "41_t",
            "42_t", "43_t", "44_t", "100_t"};

    public DatabaseHandler(){
		/* Endast för test, vet inte riktigt hur det ska se ut här. Tänker att man har
		 * en tom konstrukor och får conn genom getConnection() eller något liknande
		 */
		db = Database.getInstance();
        conn = db.getConnection();
    }

	/*------  LoginServlet ----------------*/
		/* BeanTransaction */

    /**
     * Function that checks if the user is able to log in (If username is in database and matches
     * with the entered password and the selected group).
     * @param bean A LoginBean that contains an userName, a password and a groupName.
     * @return true if the user is able to log in, else false.
     */
    public boolean loginUser(LoginBean bean){
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("SELECT * FROM Users NATURAL JOIN memberOf WHERE userName = ? AND password = ? AND groupName = ?");
            ps.setString(1, bean.getUsername());
            ps.setString(2, bean.getPassword());
            ps.setString(3, bean.getSelectedGroup());
            ResultSet rs = ps.executeQuery();
            print(ps);
            if(rs.next()){
//				System.out.print(rs.getString("username") + "\t");
//				System.out.print(rs.getString("password") + "\t");
//				System.out.println(rs.getString("groupName"));
                return true;
            }
            return false;
        } catch (SQLException e){
            System.out.println(e.getMessage());
            System.out.println(e.getSQLState());
            System.out.println(e.getErrorCode());
            return false;
        }
    }

	/*------ GroupManagementServlet -------*/
		/* BeanTransaction */

    /**
     * Function that adds a group to the database. Group name must be unique.
     * @param bean A GroupManagementBean that contains the groupName (group attribute in bean).
     * @return true if the group is added, else false (most likely because the group name already
     * exists).
     */
    public boolean addGroup(GroupManagementBean bean){
        try{
            PreparedStatement ps = conn.prepareStatement("INSERT INTO ProjectGroups(groupName) VALUES(?)");
            ps.setString(1, bean.getGroup());
            print(ps);
            if(ps.executeUpdate() > 0)
                return true;

        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

    /**
     * Deletes one or many groups from the database.
     * @param bean A GroupManagementBean that contains a list of groups (groups attribute in the bean)
     * @return true if the group(s) is deleted, else false (most likely because some of the groups doesn't
     * exist in the database.
     */
    public boolean deleteGroups(GroupManagementBean bean){
        String sql = "WHERE ";
        for(String s:bean.getGroups()){
            sql += "GroupName = ? OR ";
        }
        if(sql.endsWith(" OR ")){
            sql = sql.substring(0, sql.length()-4);
        }
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("DELETE FROM ProjectGroups " + sql);
            int i = 1;
            for(String s: bean.getGroups()){
                ps.setString(i, s);
                i++;
            }
            print(ps);
            if(ps.executeUpdate() > 0)
                return true;
        } catch(SQLException e){
            printError(e);
        }
        return false;
    }

    /**
     * Assigns a leader to a group. The user must be a member of the group.
     * @param bean A GroupManagementBean that contains the Leader and Group attributes.
     * @return true if the user is assigned leader, else false (most likely because
     * the user is not in the group).
     */
    public boolean assignLeader(GroupManagementBean bean){
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("UPDATE MemberOf set role = 'PG' WHERE userName = ? AND groupName = ?");
            ps.setString(1, bean.getLeader());
            ps.setString(2, bean.getGroup());
            print(ps);
            if(ps.executeUpdate() > 0){
                return true;
            }
            else{
                ps = conn.prepareStatement("INSERT INTO MemberOf(groupName, userName, role) VALUES(?,?, 'PG')");
                if(ps.executeUpdate() > 0){
                    return true;
                }
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
            System.out.println(e.getSQLState());
            System.out.println(e.getErrorCode());
        }
        return false;
    }

		/* BeanFactory */
    /**
     * Function that is used to fetch a list of all the groups in the database.
     * @return A GroupManagementBean that contains a List<String> of all the groups (as the groups
     * attribute in bean).
     */
    public GroupManagementBean getGroupList(){
        PreparedStatement ps = null;
        GroupManagementBean bean = new GroupManagementBean();
        try{
            ps = conn.prepareStatement("SELECT * FROM projectGroups");
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                bean.setGroups(rs.getString("groupName"));
            }
        } catch(SQLException e){
            printError(e);
        }
        return bean;
    }

    /**
     * Function that is used to fetch a list of all the users in the database.
     * @return A GroupManagementBean that contains a List<String> of all the users (as the users
     * attribute in bean).
     */
    public GroupManagementBean getUserListG(){
        PreparedStatement ps = null;
        GroupManagementBean bean = new GroupManagementBean();
        try{
            ps = conn.prepareStatement("SELECT * FROM Users");
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                bean.setUsers(rs.getString("userName"));
            }
        } catch(SQLException e){
            printError(e);
        }
        return bean;
    }

	/*------ UserManagementServlet --------*/
		/* BeanTransaction */

    /**
     * A function that adds a user to the database. Username must be unique.
     * @param bean A UserManagementBean that contains UserName, Email and Password for the new user.
     * @return true if the user is added, else false.
     */
    public boolean addUser(UserManagementBean bean){
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("INSERT INTO Users(userName, email, password) VALUES (?,?,?)");
            ps.setString(1, bean.getUserName());
            ps.setString(2, bean.getEmail());
            ps.setString(3, bean.getPassword());
            print(ps);
            if(ps.executeUpdate() > 0)
                return true;
        } catch(SQLException e){
            System.out.println(e.getMessage());
            System.out.println(e.getSQLState());
            System.out.println(e.getErrorCode());
        }
        return false;
    }

    /**
     * Deletes one or many users from the database.
     * @param bean A bean that contains a List of the users to be deleted (userList attribute in the Bean).
     * @return true if all the users are deleted, else false.
     */
    public boolean deleteUsers(UserManagementBean bean){
        String where = " WHERE";
        for(String s: bean.getUserList()){
            where += " userName = ? OR";
        }
        if(where.endsWith("OR")){
            where = where.substring(0, where.length()-2);
        }
        PreparedStatement ps = null;

        try{
            ps = conn.prepareStatement("DELETE FROM Users" + where);
            int i = 1;
            for(String s:bean.getUserList())
                ps.setString(i++, s);
            print(ps);
            if(ps.executeUpdate() > 0){
                return true;
            }
        } catch(SQLException e){
            printError(e);
        }
        return false;
    }

    /**
     * Assigns an user to a group. A user can be a member of many groups, but only one time
     * to the same group.
     * @param bean A UserManagementBean that contains an username and the group the user should be
     * assigned to (group and userName attribute in the bean).
     * @return true if the user is assigned to the group, else false.
     */
    public boolean assignGroup(UserManagementBean bean){
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("INSERT INTO memberOf(groupName, userName) VALUES(?,?)");
            ps.setString(1, bean.getGroup());
            ps.setString(2, bean.getUserName());
            print(ps);
            if(ps.executeUpdate() > 0)
                return true;
        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

    /**
     * Assigns a role to a user in a group.
     * @param bean A UserManagementBean that contains the role, username and group for which the role
     * should be assigned (role, group and userName attributes in the bean).
     * @return true if the role is assigned, else false.
     */
    public boolean assignRole(UserManagementBean bean){
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("UPDATE memberOf SET role = ? WHERE groupName = ? AND userName = ?");
            ps.setString(1, bean.getRole());
            ps.setString(2, bean.getGroup());
            ps.setString(3, bean.getUserName());
            print(ps);
            if(ps.executeUpdate() > 0)
                return true;
        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

    /**
     * Deletes a user from a group.
     * @param bean A UserManagementBean that contains the username and the group from which the
     * user should be deleted (userName and group attributes in the bean).
     * @return true if the user is deleted from the group, else false.
     */
    public boolean deleteFromGroup(UserManagementBean bean){
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("DELETE FROM MemberOf WHERE UserName = ? AND groupName = ?");
            ps.setString(1, bean.getUserName());
            ps.setString(2, bean.getGroup());
            print(ps);
            if(ps.executeUpdate() > 0){
                return true;
            }
        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

		/* BeanFactory */

    /**
     * Gets a list of all the users in the database.
     * @return A UserManagementBean containing a list of all the users (userList attribute in
     * the bean).
     */
    public UserManagementBean getUserListU(){
        UserManagementBean bean = new UserManagementBean();
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("SELECT userName FROM Users");
            print(ps);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                bean.setUserList(rs.getString("userName"));
            }
        } catch (SQLException e){
            printError(e);
        }
        return bean;
    }

	/*------ ReportManagementServlet ------*/
			/* BeanTransaction */

    /**
     * Signs one or many time reports. Only the project leader should be able to use this.
     * @param bean A ReportManagementBean that contains the group name and a map for which weeks
     * to sign for each user (group and signMap attributes in the bean).
     * <br><br>
     * << NOTE >> In the map, the key is a username and the value is a List< Integer> containing all weeks.
     * @return true if all the reports are signed, else false.
     */
    public boolean signReports(ReportManagementBean bean){
        PreparedStatement ps = null;
        String where = " WHERE";
        Map<String, List<Integer>> m = bean.getSignMap();
        for(String s:m.keySet()){
            for(int i:m.get(s)){
                where += " groupName = ? AND user = ? AND Week = ? OR";
            }
        }
        if(where.endsWith("OR"))
            where = where.substring(0, where.length()-2);
        try{
            ps = conn.prepareStatement("UPDATE TimeReports SET Signed = TRUE" + where);
            int c = 1;
            for(String s:m.keySet()){
                for(int i:m.get(s)){
                    ps.setString(c++, bean.getGroup());
                    ps.setString(c++, s);
                    ps.setInt(c++, i);
                }
            }
            print(ps);
            if(ps.executeUpdate() > 0){
                return true;
            }
        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

    /**
     * Unsigns one or many reports. Only the project leader should be able to do this.
     * @param bean A ReportManagementBean that contains the group name and a map for which weeks
     * to unsign for each user (group and signMap attributes in the bean).
     * <br><br>
     * << NOTE >> In the map, the key is a username and the value is a List< Integer> containing all weeks.
     * @return true if all the reports are unsigned, else false.
     */
    public boolean unsignReports(ReportManagementBean bean){
        PreparedStatement ps = null;
        String where = " WHERE";
        Map<String, List<Integer>> m = bean.getSignMap();
        for(String s:m.keySet()){
            for(int i:m.get(s)){
                where += " groupName = ? AND user = ? AND Week = ? OR";
            }
        }
        if(where.endsWith("OR"))
            where = where.substring(0, where.length()-2);
        try{
            ps = conn.prepareStatement("UPDATE TimeReports SET Signed = FALSE" + where);
            int c = 1;
            for(String s:m.keySet()){
                for(int i:m.get(s)){
                    ps.setString(c++, bean.getGroup());
                    ps.setString(c++, s);
                    ps.setInt(c++, i);
                }
            }
            print(ps);
            if(ps.executeUpdate() > 0){
                return true;
            }
        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

	/*------ ReportServlet ----------------*/
			/* BeanTransaction */

    /**
     * Adds a time report to the database. Only adds the values that is in the report Map
     * @param bean A ReportBean containing username, group, week, and all the columns that
     * should be added to the database (user, group, week and reportValues in the bean).
     * @return true if the time report is added, else false.
     */
    public boolean createTimeReport(ReportBean bean){
        String columns = "(user, groupname, week,";
        String values = "VALUES(?, ?, ?,";
        Map<String, Integer> rep = bean.getReportValues();
        for(String s:rep.keySet()){
            columns += s + ", ";
            values += "?,";
        }

        values = values.substring(0, values.lastIndexOf("?")+1);
        columns = columns.substring(0, columns.lastIndexOf(","));
        columns += ")";
        values += ")";
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("INSERT INTO TimeReports" + columns + " " + values);
            int i = 1;
            ps.setString(i++, bean.getUser());
            ps.setString(i++, bean.getGroup());
            ps.setInt(i++, bean.getWeek());
            for(String s : rep.keySet()){
                ps.setInt(i++, rep.get(s));
            }
            print(ps);
            if(ps.executeUpdate()>0)
                return true;

        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

    /**
     * Updates a time report in the database with new values.
     * @param bean A ReportBean containing username, group, week and all the columns that
     * should be updated in the database (user, group, week and reportValues in the bean).
     * @return true if the time report is updated, else false.
     */
    public boolean updateTimeReports(ReportBean bean){
        return removeTimeReport(bean) && createTimeReport(bean);
    }

    /**
     * Removes a time report from the database.
     * @param bean A ReportBean that contains group, username and week for which the time report
     * should be deleted (group, user and week in the bean).
     * @return true if the time report is deleted, else false.
     */
    public boolean removeTimeReport(ReportBean bean){
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("DELETE FROM TimeReports WHERE groupName = ? AND user = ? AND week = ?");
            ps.setString(1, bean.getGroup());
            ps.setString(2, bean.getUser());
            ps.setInt(3, bean.getWeek());
            if(ps.executeUpdate() > 0){
                return true;
            }
        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

	/*------ UserServlet ------------------*/
		/* BeanTransaction */

    /**
     * Changes the password for a user.
     * @param bean A UserBean that contains username and the new password to use (userName and
     * password attributes in the bean).
     * @return true if the password is changed, else false.
     */
    public boolean changePassword(UserBean bean){
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("UPDATE Users SET password = ? WHERE userName = ?");
            ps.setString(1, bean.getNewPassword());
            ps.setString(2, bean.getUserName());
            print(ps);
            if(ps.executeUpdate() > 0){
                return true;
            }
        } catch (SQLException e){
            printError(e);
        }
        return false;
    }

		/* BeanFactory */

    /**
     * Returns a list containing all groups a user is a member of.
     * @param user the user for which to give the list.
     * @return A UserBean that contains a list of all groups the user is member of (groupList attribute
     * in the bean).
     */
    public UserBean getMemberOf(String user){
        UserBean bean = new UserBean();
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("SELECT groupName FROM memberOf WHERE userName = ?");
            ps.setString(1, user);
            print(ps);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
                bean.setGroupList(rs.getString("groupName"));
        } catch (SQLException e){
            printError(e);
        }

        return bean;
    }

	/*------ DashboardServlet -------------*/
		/* BeanFactory */
    /**
     * Gets a time report or a time report summary. This method can be called in
     * a number of different ways and give different results. This depends on which
     * parameters are filled.
     * @param group The group for which the summary is to. <b><i><u>(This parameter should always be filled)</u></i></b>.
     * @param user The user for which the summary are formed after. <i>(This parameter can be marked as unfilled with <u>""</u>)</i>
     * @param role The role for which the summary are formed after. <i>(This parameter can be marked as unfilled with <u>""</u>)</i>
     * @param week The week for which the summary are formed after. <i>(This parameter can be marked as unfilled with <u>0</u>)</i>
     * @return A DashboardBean containing all columns that are present in a time report (even those with values 0).
     * The columns are placed in a Map with the column as the key and the amount of time as the value (reportValues attribute
     * in the bean).
     */
    public DashboardBean getTimeReport(String group, String user, String role, int week){
        DashboardBean bean = new DashboardBean();
        PreparedStatement ps = null;
        group = group == null? "":group;
        user = user == null? "":user;
        role = role == null? "":role;
        try{
            ps = conn.prepareStatement("CALL sumHelp(?, ?, ?, ?)");
            ps.setString(1, group);
            ps.setString(2, user);
            ps.setString(3, role);
            ps.setInt(4, week);
            print(ps);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                for(String s:TIMEREPORTCOLUMNS){
                    bean.setReportValues(s, rs.getInt(s));
                }
            }
        } catch (SQLException e){
            printError(e);
        }
        return bean;
    }

    /**
     * Gives all time reported to a specific document (e.g 11,12...)
     * @param group the group for which to summarize the document.
     * @param document the number for the document.
     * @return A DashboardBean containing an integer describing the time reported to the document (
     * documentSummary attribute in the bean).
     */
    public DashboardBean getDocumentSummary(String group, int document){
        DashboardBean bean = new DashboardBean();
        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement("SELECT SUM(?_t) as sum FROM TimeReports WHERE groupName = ?");
            ps.setInt(1, document);
            ps.setString(2, group);
            print(ps);
            ResultSet rs = ps.executeQuery();
            if(rs.next())
                bean.setDocumentSummary(rs.getInt("sum"));

        } catch (SQLException e){
            printError(e);
        }
        return bean;
    }

    /**
     * Gives all time reported to a specific activity (e.g d, i,....)
     * @param group the group for which to summarize the document.
     * @param activity the letter for the activity.
     * @return A DashboardBean containing an integer describing the time reported to the activity (
     * activitySummary attribute in the bean).
     */
    public DashboardBean getActivitySummary(String group, String activity){
        DashboardBean bean = new DashboardBean();
        if(activity.contains("'"))
            return bean;
        PreparedStatement ps = null;
        int sum = 0;
        try{
            ps = conn.prepareStatement("SELECT SUM(?_" + activity + ") as sum FROM TimeReports WHERE groupName = ?");
            ps.setString(2, group);
            ResultSet rs = null;
            for(int i = 11; i<20; i++){
                ps.setInt(1, i);
                print(ps);
                rs = ps.executeQuery();
                if(rs.next()){
                    sum += rs.getInt("sum");
                }
            }
            bean.setActivitySummary(sum);
        } catch(SQLException e){
            printError(e);
        }
        return bean;
    }


    public void disconnect(){
        db.close();
    }

    private void print(PreparedStatement ps){
        String s = ps.toString();
        int i = 0;
        if(s.indexOf("SELECT") >= 0)
            i = s.indexOf("SELECT");
        else if(s.indexOf("UPDATE") >= 0)
            i = s.indexOf("UPDATE");
        else if(s.indexOf("DELETE") >= 0)
            i = s.indexOf("DELETE");
        else if(s.indexOf("INSERT") >=0)
            i = s.indexOf("INSERT");
        else if(s.indexOf("CALL") >= 0)
            i = s.indexOf("CALL");

        System.out.println(s.substring(i));
    }

    private void printError(SQLException e){
        System.out.println(e.getMessage());
    }
}
