package main.java.service;

import main.java.model.Account;
import main.java.model.AccountType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class AccountService {
    private final List<Account> accounts = new ArrayList<>();

    public Account addAccount(String description, double value, LocalDate dueDate, AccountType type) {
        if(value <= 0){
            throw new IllegalArgumentException("Value must be positive.");
        }

        if(description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cant be empty.");
        }

        Account account = new Account(description, value, dueDate, type);
        accounts.add(account);
        return account;
    }

    public void markAsPaid(UUID accountId) {
        getAccountById(accountId).ifPresentOrElse(
                account -> account.setPaid(true),
                () -> { throw new IllegalArgumentException("Account not found."); }
        );
    }

    public Optional<Account> getAccountById(UUID accountId) {
        return accounts.stream().filter(
        account -> account.getId().equals(accountId)).findFirst();
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts);
    }

    public List<Account> getAccountsByType(AccountType type) {
        return accounts.stream()
            .filter(account -> account.getType() == type).collect(Collectors.toList());
    }

    public List<Account> getPendingAccounts() {
        return accounts.stream()
            .filter(account -> !account.isPaid()).collect(Collectors.toList());
    }

    public List<Account> getOverdueAccounts() {
        LocalDate today = LocalDate.now();
        return accounts.stream()
                .filter(account -> !account.isPaid())
                .filter(account -> account.getDueDate().isBefore(today))
                .collect(Collectors.toList());
    }

    public String getAccountStatus(UUID accountId) {
        return getAccountById(accountId).map(account -> {
            if(account.isPaid()) {
                return "Paid";
            } else if(account.getDueDate().isBefore(LocalDate.now())){
                return "Overdue";
            }
            return "Pending";
        }).orElse("Account not found.");
    }

    public void LoadSampleData() {
        addAccount("Aluguel", 1500.00, LocalDate.now().plusDays(30), AccountType.PAYABLE);
        addAccount("Salário", 5000.0, LocalDate.now().plusDays(5), AccountType.RECEIVABLE);
        addAccount("Internet", 120.9, LocalDate.now().minusDays(10), AccountType.PAYABLE);

        accounts.stream()
                .filter(account -> account.getDescription().equals("Internet"))
                .findFirst()
                .ifPresent(account -> account.setPaid(true));
    }







}
