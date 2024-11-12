package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.*;
import scheduler.util.Util;

import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Arrays;


public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (!checkPassword(password)) {
            return;
        }
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (!checkPassword(password)) {
            return;
        }
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static boolean checkPassword(String password) {
        if (password.length() < 8) {
            System.out.println("The password should contain at least 8 characters!");
            return false;
        }
        int lowercase = 0;
        int uppercase = 0;
        int number = 0;
        int specialCharacter = 0;
        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);
            if (character >= 'a' && character <= 'z') {
                lowercase += 1;
            } else if (character >= 'A' && character <= 'Z') {
                uppercase += 1;
            } else if (Character.isDigit(character)) {
                number += 1;
            } else if (character == '!' || character == '@' || character == '#' || character == '?') {
                specialCharacter += 1;
            }
        }
        if (uppercase == 0 || lowercase == 0) {
            System.out.println("Strong password contains uppercase and lowercase letters.");
            return false;
        }
        if (number == 0) {
            System.out.println("Strong password contains numbers and letters.");
            return false;
        }
        if (specialCharacter == 0) {
            System.out.println("Strong password containss at least one special character.");
            return false;
        }
        return true;
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if(currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            PreparedStatement statementCaregiver = con.prepareStatement("SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC");
            PreparedStatement statementVaccine = con.prepareStatement("SELECT * FROM Vaccines");
            Date day = Date.valueOf(date);
            statementCaregiver.setDate(1, day);

            ResultSet resultSet_Caregiver = statementCaregiver.executeQuery();
            ResultSet resultSet_Vaccine = statementVaccine.executeQuery();
            System.out.println("Available appointments on:" + date);
            while(resultSet_Caregiver.next()) {
                System.out.println("Caregiver: " + resultSet_Caregiver.getString("Username") + " ");
            }
            while(resultSet_Vaccine.next()) {
                System.out.print("Available vaccines: " + resultSet_Vaccine.getString("Name")+ "||");
                System.out.println("Available doses: " + resultSet_Vaccine.getString("Doses"));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first!");
            return;
        }

        if (currentPatient == null) {
            System.out.println("Please login as a patient!");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String aptDate = tokens[1];
        String vaccineName = tokens[2];

        Vaccine vaccine = null;
        String availableCaregiver = "";

        String patientName = currentPatient.getUsername();
        String aptID = "SELECT MAX(Appointment_id) FROM Appointments";
        try {
            PreparedStatement statement = con.prepareStatement(aptID);
            ResultSet resultSet = statement.executeQuery();

            int Appointment_id = 0;
            if (resultSet.next()) {
                Appointment_id = resultSet.getInt(1);
            }
            aptID = Integer.toString(Appointment_id);
        } catch (SQLException e) {
            e.printStackTrace();

        } finally {
            cm.closeConnection();
        }

        try {
            Date date = Date.valueOf(aptDate);
            Availability availability = new Availability.GetAvailability(date).get();
            if (availability == null) {
                System.out.println("There is no caregiver.");
                return;
            }
            availableCaregiver = availability.getUsername();
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
            int doses = vaccine.getAvailableDoses();
            if (doses < 1) {
                System.out.println("Not enough available doses!");
                return;
            }
            System.out.println("Successfully made the reservation in " + aptDate + ". \n \nHere is your appointment information: ");
            System.out.print("Appointment ID: {" + aptID + "}, Caregiver username: {" + availableCaregiver + "}");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        ConnectionManager cm1 = new ConnectionManager();
        Connection con1 = cm1.createConnection();

        try {
            PreparedStatement updateStatement = con1.prepareStatement("INSERT INTO Appointments VALUES (?, ?, ?, ?)");
            updateStatement.setString(1, patientName);
            updateStatement.setString(2, availableCaregiver);
            updateStatement.setString(3, vaccineName);
            updateStatement.setString(4, aptDate );
            updateStatement.executeUpdate();

            PreparedStatement updateAvailabilityStatement = con1.prepareStatement("DELETE FROM Availabilities WHERE Time = ? AND Username = ?");
            updateAvailabilityStatement.setString(1, aptDate);
            updateAvailabilityStatement.setString(2, availableCaregiver);
            updateAvailabilityStatement.executeUpdate();

            vaccine.decreaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm1.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }


    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if(currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String patient = "SELECT Appointment_id, Vaccine_Name, Appointment_time, Caregiver_Username FROM Appointments WHERE Caregiver_Username = ? ORDER BY Appointment_id ASC";
        String caregiver = "SELECT Appointment_id, Vaccine_Name, Appointment_time, Patient_Username FROM Appointments WHERE Patient_Username = ? ORDER BY Appointment_id ASC";
        try {
            if (currentCaregiver == null) {
                PreparedStatement statement1 = con.prepareStatement(patient);
                statement1.setString(1, currentPatient.getUsername());
                ResultSet resultSet1 = statement1.executeQuery();
                while (resultSet1.next()) {
                    System.out.print("Appointment ID: " + resultSet1.getInt("Appointment_id") + " ");
                    System.out.print("Vaccine name: " + resultSet1.getString("Vaccine_Name") + " ");
                    System.out.print("Date: " + resultSet1.getString("Appointment_time") + " ");
                    System.out.println("Patient name: " + resultSet1.getString("Caregiver_Username"));
                }
            } else {
                PreparedStatement statement2 = con.prepareStatement(caregiver);
                statement2.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet2 = statement2.executeQuery();
                while (resultSet2.next()) {
                    System.out.print("Appointment ID: " + resultSet2.getInt("Appointment_id") + " ");
                    System.out.print("Vaccine name : " + resultSet2.getString("Vaccine_Name") + " ");
                    System.out.print("Date: " + resultSet2.getString("Appointment_time") + " ");
                    System.out.println("Patient name:" + resultSet2.getString("Patient_Username"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        currentPatient = null;
        currentCaregiver = null;
        System.out.println("Successfully logged out!");
    }
}