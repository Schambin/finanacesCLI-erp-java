package main;

import main.java.model.Account;
import main.java.model.AccountType;
import main.java.service.AccountService;
import main.java.service.SummaryService;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    private final AccountService accountService = new AccountService();
    private final SummaryService summaryService = new SummaryService(accountService);
    private final Scanner scanner = new Scanner(System.in);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        accountService.loadSampleData();
        showMainMenu();
    }


    private void showMainMenu() {
        while(true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. Add Account");
            System.out.println("2. List Accounts");
            System.out.println("3. Mark Payable as Paid");
            System.out.println("4. Mark Receivable as Received");
            System.out.println("5. Search by ID");
            System.out.println("6. Summary");
            System.out.println("7. Exit");
            System.out.print("Choose an option: ");

            try {
                int option = scanner.nextInt();
                scanner.nextLine();

                switch (option){
                    case 1 -> addAccount();
                    case 2 -> listAllAccounts();
                    case 3 -> markAsPaid();
                    case 4 -> markAsReceived();
                    case 5 -> searchAccountById();
                    case 6 -> showSummary();
                    case 7 -> {
                        System.out.println("Shutting Down...");
                        System.exit(0);
                    }
                    default -> System.out.println("Invalid Option");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input! Type a number.");
                scanner.nextLine();
            }
        }
    }

    private void addAccount() {
        System.out.println("\n=== New Account ===");

        String description = getInput(
            "Description: ",
            "Description cant be empty",
            input -> !input.trim().isEmpty()
        );

        double value = Double.parseDouble(getInput(
                "Value (ex 150.50): ",
                "Invalid Value",
                input -> input.matches("^\\d+(\\.\\d+)?$") && Double.parseDouble(input) > 0)
        );

        LocalDate dueDate = LocalDate.parse(getInput(
            "Due Date (YYYY-MM-DD): ",
                "Invalid Date",
                input -> {
                    try {
                        LocalDate.parse(input, dateFormatter);
                        return true;
                    } catch (DateTimeException e) {
                        return false;
                    }
                }), dateFormatter);
            AccountType type = getAccountType();
            Account account = accountService.addAccount(description,value, dueDate, type);
    }

    private void listAllAccounts() {
        System.out.println("\n=== All Accounts ===");

        System.out.println("\n--- Payables ---");
        System.out.println("Pending:");
        printAccountList(accountService.getNumberedPendingPayables());

        System.out.println("\nPaid:");
        printAccountList(accountService.getNumberedPaidPayables());

        System.out.println("\n--- Receivables ---");
        System.out.println("Pending:");
        printAccountList(accountService.getNumberedPendingReceivables());

        System.out.println("\nReceived:");
        printAccountList(accountService.getNumberedPaidReceivables());
    }

    private void printAccountList(Map<Integer, Account> accounts) {
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }

        accounts.forEach((id, account) -> {
            String status = account.isPaid() ? "Paid" : "Pending";
            String overdue = account.getDueDate().isBefore(LocalDate.now()) && !account.isPaid()
                    ? "[OVERDUE] " : "";

            System.out.printf("%d. %s%s - R$ %.2f (Due: %s) - %s\n",
                    id,
                    overdue,
                    account.getDescription(),
                    account.getValue(),
                    account.getDueDate().format(dateFormatter),
                    status);
        });
    }

    private void markAsPaid() {
        System.out.println("\n=== Mark Payable as Paid ===");
        Map<Integer, Account> pendingPayables = accountService.getNumberedPendingPayables();

        if (pendingPayables.isEmpty()) {
            System.out.println("No pending accounts to pay.");
            return;
        }

        System.out.println("Pending Payables:");
        pendingPayables.forEach((id, account) ->
                System.out.printf("%d. %s - R$ %.2f (Due: %s)\n",
                        id,
                        account.getDescription(),
                        account.getValue(),
                        account.getDueDate().format(dateFormatter))
        );

        String input = getInput(
                "\nType the payable NUMBER to mark as paid: ",
                "Invalid number",
                inputStr -> {
                    try {
                        int num = Integer.parseInt(inputStr);
                        return pendingPayables.containsKey(num);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
        );

        accountService.markAsPaid(input);
        System.out.println("Payable marked as paid successfully!");
    }

    private void markAsReceived() {
        System.out.println("\n=== Mark Receivable as Received ===");
        Map<Integer, Account> pendingReceivables = accountService.getNumberedPendingReceivables();

        if (pendingReceivables.isEmpty()) {
            System.out.println("No pending receivables.");
            return;
        }

        System.out.println("Pending Receivables:");
        pendingReceivables.forEach((id, account) ->
                System.out.printf("%d. %s - R$ %.2f (Due: %s)\n",
                        id,
                        account.getDescription(),
                        account.getValue(),
                        account.getDueDate().format(dateFormatter))
        );

        String input = getInput(
                "\nType the receivable NUMBER to mark as received: ",
                "Invalid number",
                inputStr -> {
                    try {
                        int num = Integer.parseInt(inputStr);
                        return pendingReceivables.containsKey(num);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
        );

        accountService.markAsReceived(input);
        System.out.println("Receivable marked as received successfully!");
    }

    private void showSummary() {
        System.out.println(summaryService.generateFullReport());

        List<Account> overdue = accountService.getOverdueAccounts();
        if (!overdue.isEmpty()) {
            System.out.println("\n⚠️ Overdue Accounts:");
            overdue.forEach(acc -> System.out.printf(
                    "- %s: R$ %.2f (Expired in %s)%n",
                    acc.getDescription(),
                    acc.getValue(),
                    acc.getDueDate().format(dateFormatter)
            ));
        }
    }

    private void searchAccountById() {
        System.out.println("\n=== Search by ID ===");
        String accountId = getInput("Type Account ID: ",
                "Invalid ID", input -> {
                    try {
                        UUID.fromString(input);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                });

        // Use findAccountById para string ou getAccountById para UUID
        accountService.getAccountById(UUID.fromString(accountId)).ifPresentOrElse(
                account -> {
                    System.out.println("\n=== Account Found ===");
                    System.out.printf("ID: %s\n", account.getId());
                    System.out.printf("Description: %s\n", account.getDescription());
                    System.out.printf("Value: R$ %.2f\n", account.getValue());
                    System.out.printf("Due Date: %s\n", account.getDueDate().format(dateFormatter));
                    System.out.printf("Type: %s\n", account.getType());
                    System.out.printf("Status: %s\n", accountService.getAccountStatus(account.getId()));
                },
                () -> System.out.println("Account not found!")
        );
    }

    // === Assist Methods ===
    private AccountType getAccountType() {
        while(true) {
            System.out.println("Type (1 - Pay, 2 - Receive)");
            String input = scanner.nextLine();

            if(input.equals("1")) return AccountType.PAYABLE;
            if(input.equals("2")) return AccountType.RECEIVABLE;

            System.out.println("Invalid Option! Type 1 or 2.");
        }
    }

    private String getInput(String prompt, String errorMessage, java.util.function.Predicate<String> validator) {
        while(true) {
            System.out.printf(prompt);
            String input = scanner.nextLine();

            if(validator.test(input)){
                return input;
            }

            System.out.println(errorMessage);
        }
    }

}