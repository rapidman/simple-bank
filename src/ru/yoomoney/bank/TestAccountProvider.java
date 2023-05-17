package ru.yoomoney.bank;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import ru.yoomoney.bank.BankSimple.Account;

/**
 * @author timur
 * @since 18.05.2023
 */
class TestAccountProvider {

  private double INITIAL_AMOUNT = 1_000_000;

  public List<Account> createAccounts(int count) {
    List<Account> accounts = new ArrayList<>(count);
    for (int i = 1; i <= count; i++) {
      accounts.add(new Account(i, new BigDecimal(INITIAL_AMOUNT)));
    }
    return accounts;
  }
}
