package scheduler.model;

import scheduler.db.ConnectionManager;
import java.sql.*;

public class Appointment {
    private final int id;
    private final Date date;
    private final String caregiver;
    private final String patient;
    private final String vaccine;

    public Appointment (AppointmentBuilder builder) {
        this.id = builder.id;
        this.date = builder.date;
        this.caregiver = builder.caregiver;
        this.patient = builder.patient;
        this.vaccine = builder.vaccine;
    }

    public Appointment (AppointmentGetter getter) {
        this.id = getter.id;
        this.date = getter.date;
        this.caregiver = getter.caregiver;
        this.patient = getter.patient;
        this.vaccine = getter.vaccine;
    }

    public int getID () { return id; }

    public Date getDate () { return date; }

    public String getCaregiver () { return caregiver; }

    public String getPatient () { return patient; }

    public String getVaccine () { return vaccine; }

    public void makeAppointment () throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String makeApt = "INSERT INTO Appointments VALUES (?,?,?,?,?)";
        try {
            PreparedStatement statement = con.prepareStatement(makeApt);
            statement.setInt(1, id);
            statement.setDate(2, date);
            statement.setString(3, caregiver);
            statement.setString(4, patient);
            statement.setString(5, vaccine);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void cancelAppointment (int id) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String cancelAppt = "DELETE FROM Appointments WHERE id = ?";
        try {
            PreparedStatement statement = con.prepareStatement(cancelAppt);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentBuilder {
        private final int id;
        private final Date date;
        private final String caregiver;
        private final String patient;
        private final String vaccine;

        public AppointmentBuilder(Date date, String caregiver, String patient, String vaccine) throws SQLException {
            this.id = generateID();
            this.date = date;
            this.caregiver = caregiver;
            this.patient = patient;
            this.vaccine = vaccine;
        }
        // increments max(id) by 1 to generate unique appointment ids
        public int generateID () throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            String aptID = "SELECT MAX(id) AS id FROM Appointments";
            try {
                PreparedStatement statement = con.prepareStatement(aptID);
                ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                return resultSet.getInt("id") + 1;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }

        public Appointment build() {
            return new Appointment(this);
        }
    }

    public static class AppointmentGetter {
        private final int id;
        private Date date;
        private String caregiver;
        private String patient;
        private String vaccine;

        public AppointmentGetter(int id) {
            this.id = id;
        }

        public Appointment get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            String getAppointment = "SELECT * FROM Appointments WHERE id = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getAppointment);
                statement.setInt(1, this.id);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    this.date = resultSet.getDate("Time");
                    this.caregiver = resultSet.getString("Caregiver");
                    this.patient = resultSet.getString("Patient");
                    this.vaccine = resultSet.getString("Vaccine");
                    return new Appointment(this);
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