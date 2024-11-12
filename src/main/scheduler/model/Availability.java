package scheduler.model;

import scheduler.db.ConnectionManager;
import java.sql.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Availability {
    private final Date apt_time;
    private final String username;


    private Availability(GetAvailability getter){
        this.apt_time = getter.date;
        this.username = getter.caregiver;
    }


    public Date getTime() {
        return apt_time;
    }

    public String getUsername() {
        return username;
    }

    public static class GetAvailability {
        private final Date date;
        private String caregiver;

        public GetAvailability(Date date) {
            this.date = date;
        }

        public Availability get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String Availability = "SELECT Time, Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
            try {
                PreparedStatement statement = con.prepareStatement(Availability);
                statement.setDate(1, date);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    this.caregiver = resultSet.getString("Username");
                    return new Availability(this);
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}