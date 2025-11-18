CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (Username VARCHAR(255) PRIMARY KEY,
                       Salt BINARY(16),
                       Hash BINARY(16));

CREATE TABLE Appointments(Appointment_ID INT PRIMARY KEY,
                          Caregiver_Username VARCHAR(255),
                          Patient_Username VARCHAR(255),
                          Vaccine VARCHAR(255),
                          Time date,
                          FOREIGN KEY (Time, Caregiver_Username) REFERENCES Availabilities(Time, Username),
                          FOREIGN KEY (Patient_Username) REFERENCES Patients(Username),
                          FOREIGN KEY (Vaccine) REFERENCES Vaccines(Name));
