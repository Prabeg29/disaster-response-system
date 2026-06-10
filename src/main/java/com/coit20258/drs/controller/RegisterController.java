package com.coit20258.drs.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

import com.coit20258.drs.model.Department;
import com.coit20258.drs.model.User;
import com.coit20258.drs.service.AppService;
import com.coit20258.drs.util.Security;
import com.coit20258.drs.util.SceneManager;

public class RegisterController implements Initializable {

    private static final Logger LOGGER
            = Logger.getLogger(RegisterController.class.getName());

    private static final Pattern EMAIL_PATTERN
            = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int MIN_PASSWORD_LENGTH = 8;

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ComboBox<String> roleCombo;

    @FXML
    private Label firstNameError;
    @FXML
    private Label lastNameError;
    @FXML
    private Label emailError;
    @FXML
    private Label passwordError;
    @FXML
    private Label confirmPasswordError;

    @FXML
    private Label feedbackLabel;
    @FXML
    private Button registerButton;
    @FXML
    private VBox departmentSection;
    @FXML
    private ComboBox<Department> departmentCombo;

    private final AppService service = AppService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roleCombo.setItems(FXCollections.observableArrayList(
                "Reporter",
                "Operator",
                "Admin",
                "Department Coordinator"
        ));
        roleCombo.getSelectionModel().selectFirst();

        // Show/hide department selector based on role
        departmentSection.setManaged(false);
        departmentSection.setVisible(false);
        roleCombo.valueProperty().addListener((obs, old, val) -> {
            boolean isDept = "Department Coordinator".equals(val);
            departmentSection.setManaged(isDept);
            departmentSection.setVisible(isDept);
        });

        // Load departments in background for the combo
        new Thread(() -> {
            try {
                List<Department> depts = service.findAllDepartments();
                Platform.runLater(() -> departmentCombo.setItems(
                        FXCollections.observableArrayList(depts)));
            } catch (Exception ignored) {}
        }, "load-depts-thread").start();

        clearAllErrors();

        emailField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) checkEmailAvailability();
        });

        confirmPasswordField.textProperty().addListener((obs, old, val) -> {
            if (!val.isEmpty()) validatePasswordMatch(false);
        });
    }

    @FXML
    private void handleRegister() {
        clearAllErrors();
        hideFeedback();

        if (!validateAllFields()) {
            return;       // errors already displayed inline
        }

        registerButton.setDisable(true);
        registerButton.setText("Creating account…");

        new Thread(() -> {
            try {
                // ── Final uniqueness check via server ─────────────────────────
                boolean emailTaken = service.emailExists(emailField.getText().trim());

                if (emailTaken) {
                    Platform.runLater(() -> {
                        registerButton.setDisable(false);
                        registerButton.setText("Create Account");
                        showFieldError(emailError, "This email is already registered.");
                    });
                    return;
                }

                String roleConst = labelToRoleConstant(roleCombo.getValue());

                User newUser = new User(
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        emailField.getText().trim().toLowerCase(),
                        Security.hashPassword(passwordField.getText()),
                        roleConst
                );

                // Link to department if registering as Department Coordinator
                if (User.ROLE_DEPARTMENT.equals(roleConst)) {
                    Department selected = departmentCombo.getValue();
                    if (selected != null) newUser.setDepartmentId(selected.getId());
                }

                service.register(newUser);

                LOGGER.info("New user registered: " + newUser.getEmail());

                // ── Navigate back to login with a success message ─────────────
                Platform.runLater(() -> {
                    LoginController loginCtrl
                            = SceneManager.switchToWithController("LoginView");
                    loginCtrl.showSuccess(
                            "✔  Account created! You can now sign in, "
                            + newUser.getFirstName() + ".");
                });

            } catch (Exception ex) {
                LOGGER.severe("Registration error: " + ex.getMessage());
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("Create Account");
                    showGlobalError("Registration failed: " + ex.getMessage());
                });
            }
        }, "register-thread").start();
    }

    @FXML
    private void handleClear() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        roleCombo.getSelectionModel().clearSelection();
        clearAllErrors();
        hideFeedback();
    }

    @FXML
    private void handleBackToLogin() {
        SceneManager.switchTo("LoginView");
    }

    private boolean validateAllFields() {
        boolean ok = true;

        // First name
        if (firstNameField.getText().trim().length() < 2) {
            showFieldError(firstNameError, "First name must be at least 2 characters.");
            ok = false;
        }

        // Last name
        if (lastNameField.getText().trim().length() < 2) {
            showFieldError(lastNameError, "Last name must be at least 2 characters.");
            ok = false;
        }

        // Email
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showFieldError(emailError, "Email address is required.");
            ok = false;
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            showFieldError(emailError, "Enter a valid email address.");
            ok = false;
        }

        // Password
        String password = passwordField.getText();
        if (password.isEmpty()) {
            showFieldError(passwordError, "Password is required.");
            ok = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            showFieldError(passwordError,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            ok = false;
        }

        // Confirm password
        if (!validatePasswordMatch(true)) {
            ok = false;
        }

        // Role
        if (roleCombo.getValue() == null) {
            showGlobalError("Please select an account role.");
            ok = false;
        }

        // Department required for Department Coordinator role
        if ("Department Coordinator".equals(roleCombo.getValue())
                && departmentCombo.getValue() == null) {
            showGlobalError("Please select a department.");
            ok = false;
        }

        return ok;
    }

    private boolean validatePasswordMatch(boolean showIfBlank) {
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (confirm.isEmpty()) {
            if (showIfBlank) {
                showFieldError(confirmPasswordError, "Please confirm your password.");
                return false;
            }
            return true;    // not yet typed — don't flag yet
        }

        if (!pass.equals(confirm)) {
            showFieldError(confirmPasswordError, "Passwords do not match.");
            return false;
        }

        hideFieldError(confirmPasswordError);
        return true;
    }

    private void checkEmailAvailability() {
        String email = emailField.getText().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return;
        }

        new Thread(() -> {
            boolean taken = service.emailExists(email);
            Platform.runLater(() -> {
                if (taken) {
                    showFieldError(emailError, "Email already registered.");
                } else {
                    hideFieldError(emailError);
                }
            });
        }, "email-check-thread").start();
    }

    private String labelToRoleConstant(String label) {
        return switch (label) {
            case "Operator"               -> User.ROLE_OPERATOR;
            case "Admin"                  -> User.ROLE_ADMIN;
            case "Department Coordinator" -> User.ROLE_DEPARTMENT;
            default                       -> User.ROLE_REPORTER;
        };
    }

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideFieldError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }

    private void clearAllErrors() {
        hideFieldError(firstNameError);
        hideFieldError(lastNameError);
        hideFieldError(emailError);
        hideFieldError(passwordError);
        hideFieldError(confirmPasswordError);
    }

    private void showGlobalError(String message) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("success-label");
        if (!feedbackLabel.getStyleClass().contains("validation-label")) {
            feedbackLabel.getStyleClass().add("validation-label");
        }
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
    }

    private void hideFeedback() {
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        feedbackLabel.setText("");
    }
}
