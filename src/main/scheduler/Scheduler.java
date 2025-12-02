package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;

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
        if(tokens.length != 3){
            System.out.println("Create patient failed");
            return;
        }
        String patientUsername = tokens[1];
        String patientPassword = tokens[2];

        if(patientUsernameExists(patientUsername)){
            System.out.println("Username taken, try again");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(patientPassword, salt);

        try {
            Patient patient = new Patient.PatientBuilder(patientUsername, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + patientUsername);
        } catch (SQLException e){
            System.out.println("Create user failed");
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
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
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean patientUsernameExists(String patientUsername) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectPatientUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectPatientUsername);
            statement.setString(1, patientUsername);
            ResultSet resultSet = statement.executeQuery();

            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Create patient failed");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String patientUsername = tokens[1];
        String patientPassword = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(patientUsername, patientPassword).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
        }
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as " + patientUsername);
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
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        if(tokens.length != 2){
            System.out.println("Please try again");
            return;
        }

        if(currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        System.out.println("Caregivers:");
        String date_string = tokens[1];
        Date date = Date.valueOf(date_string);
        String availableCaregivers = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";

        try {
            PreparedStatement statement = con.prepareStatement(availableCaregivers);
            statement.setDate(1, date);
            ResultSet resultSet = statement.executeQuery();

            boolean nextExists = false;
            while(resultSet.next()){
                nextExists = true;
                String caregiverUsername = resultSet.getString("Username");
                System.out.println(caregiverUsername);
            }
            if (!nextExists){
                System.out.println("No caregivers available");
            }

            System.out.println("Vaccines:");
            String vaccine_counts = "SELECT Name, Doses FROM Vaccines WHERE Doses > 0 ORDER BY Name";
            statement = con.prepareStatement(vaccine_counts);
            ResultSet vaccineSet = statement.executeQuery();

            boolean nextVaccineExists = false;
            while(vaccineSet.next()) {
                nextVaccineExists = true;
                String vaccine_name = vaccineSet.getString("Name");
                int doses = vaccineSet.getInt("Doses");
                System.out.print(vaccine_name + " ");
                System.out.println(doses);
            }
            if(!nextVaccineExists){
                System.out.println("No vaccines available");
            }


        } catch (SQLException e){
            System.out.println("Please try again");
        } catch (DateTimeParseException e){
            System.out.println("Please try again");
            return;
        } catch (DateTimeException e) {
            System.out.println("Please try again");
            return;
        } finally {
            cm.closeConnection();
        }

    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        if(tokens.length != 3){
        System.out.println("Please try again");
        return;
        }

        if(currentCaregiver != null){
            System.out.println("Please login as a patient");
            return;
        } else if (currentPatient == null){
            System.out.println("Please login first");
            return;
        }

        String date_s = tokens[1];
        Date date = Date.valueOf(date_s);
        String vaccine = tokens[2];

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            String select_caregivers = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
            PreparedStatement sc_statement = con.prepareStatement(select_caregivers);
            sc_statement.setDate(1, date);
            ResultSet available_caregivers = sc_statement.executeQuery();

            if(!available_caregivers.next()){
                System.out.println("No caregiver is available");
                return;
            }

            String select_vaccine_dose = "SELECT Doses FROM Vaccines WHERE Name = ?";
            PreparedStatement sd_statement = con.prepareStatement(select_vaccine_dose);
            sd_statement.setString(1, vaccine);
            ResultSet vaccine_dose = sd_statement.executeQuery();

            if(!vaccine_dose.next()){
                System.out.println("Not enough available doses");
                return;
            }
            // get here, caregiver and vaccine are available

            available_caregivers.next();
            String caregiver = available_caregivers.getString("Username");

            String remove_caregiver = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
            PreparedStatement rc_statement = con.prepareStatement(remove_caregiver);
            rc_statement.setString(1, caregiver);
            rc_statement.setDate(2, date);
            rc_statement.executeUpdate();

            vaccine_dose.next();
            int num_doses = vaccine_dose.getInt("Doses");
            if(num_doses == 1){
                String remove_vaccine = "DELETE FROM Vaccines WHERE Name = ?";
                PreparedStatement rv_statement = con.prepareStatement(remove_vaccine);
                rv_statement.setString(1, vaccine);
                rv_statement.executeUpdate();
            } else {
                String update_doses = "UPDATE Vaccines SET Doses = ? WHERE Name = ?";
                PreparedStatement ud_statement = con.prepareStatement(update_doses);
                ud_statement.setInt(1, (num_doses-1));
                ud_statement.setString(2, vaccine);
                ud_statement.executeUpdate();
            }

            String num_apps = "SELECT COUNT(*) FROM Appointments";
            PreparedStatement na_statement = con.prepareStatement(num_apps);
            ResultSet sch_apps = na_statement.executeQuery();
            int apps_count = sch_apps.getInt("COUNT(*)");
            int app_id = apps_count + 1;

            String schedule_app = "INSERT INTO Appointments (?, ?, ?, ?, ?)";
            PreparedStatement sa_statement = con.prepareStatement(schedule_app);
            sa_statement.setInt(1, app_id);
            sa_statement.setString(2, caregiver);
            sa_statement.setString(3, currentPatient.getUsername());
            sa_statement.setString(4, vaccine);
            sa_statement.setDate(5, date);
            sa_statement.executeUpdate();

            System.out.println("Appointment ID " + app_id + ", Caregiver username " + caregiver);
            return;
        } catch (SQLException e){
            System.out.println("Please try again");
        } finally {
            cm.closeConnection();
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
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        if(tokens.length != 1){
            System.out.println("Please try again");
            return;
        }

        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            if(currentCaregiver == null) {
                String patientUsername = currentPatient.getUsername();
                String show_apps_p = "SELECT Appointment_ID, Vaccine, Time, Caregiver_Username FROM Appointments WHERE Patient_Username = ? ORDER BY Appointment_ID";
                PreparedStatement statement_p = con.prepareStatement(show_apps_p);
                statement_p.setString(1, patientUsername);
                ResultSet resultSet_p = statement_p.executeQuery();

                if(!resultSet_p.next()){
                    System.out.println("No appointments scheduled");
                    return;
                }
                while(resultSet_p.next()){
                    String app_id = resultSet_p.getString("Appointment_ID");
                    String vaccine = resultSet_p.getString("Vaccine");
                    Date date = resultSet_p.getDate("Time");
                    String caregiver = resultSet_p.getString("Caregiver_Username");
                    System.out.println(app_id + " " + vaccine + " " + date + " " + caregiver);
                }
            } else if (currentPatient == null) {
                String caregiverUsername = currentCaregiver.getUsername();
                String show_apps_c = "SELECT Appointment_ID, Vaccine, Time, Patient_Username FROM Appointments WHERE Caregiver_Username = ? ORDER BY Appointment_ID";
                PreparedStatement statement_c = con.prepareStatement(show_apps_c);
                statement_c.setString(1, caregiverUsername);
                ResultSet resultSet_c = statement_c.executeQuery();

                if (!resultSet_c.next()) {
                    System.out.println("No appointments scheduled");
                    return;
                }
                while (resultSet_c.next()) {
                    String app_id = resultSet_c.getString("Appointment_ID");
                    String vaccine = resultSet_c.getString("Vaccine");
                    Date date = resultSet_c.getDate("Time");
                    String patient = resultSet_c.getString("Patient_Username");
                    System.out.println(app_id + " " + vaccine + " " + date + " " + patient);
                }
            }
        } catch (SQLException e){
            System.out.println("Please try again");
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        if(tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        if(currentCaregiver == null && currentPatient == null){
            System.out.println("Please login first");
            return;
        }

        try {
            if (currentCaregiver != null) {
                currentCaregiver = null;
            } else if (currentPatient != null) {
                currentPatient = null;
            }
            System.out.println("Successfully logged out");
            return;
        } catch (IllegalArgumentException e){
            System.out.println("Please try again");
            return;
        }
    }
}
