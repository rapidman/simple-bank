package ru.yoomoney.bank;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ru.yoomoney.bank.BankSimple.Account;
import ru.yoomoney.bank.BankSimple.HistoryItem;
import ru.yoomoney.bank.BankSimple.Period;

/**
 * @author timur
 * @since 18.05.2023
 */
class BankSimpleTest {

  private static final int TOTAL_ACCOUNTS_COUNT = 100;

  public static void main(String[] args) {
    TestAccountProvider accountProvider = new TestAccountProvider();
    List<Account> accounts = accountProvider.createAccounts(TOTAL_ACCOUNTS_COUNT);
    Map<Account, List<HistoryItem>> history = new HashMap<>();
    BankSimple bankSimple = new BankSimple(accounts, history);
    bankSimple.transfer(1, 2, new BigDecimal(1_000));
    dumpHistory(history);
    dumpHistory(bankSimple.getAccountStatistic(1, Period.DAY));
    dumpHistory(bankSimple.getAccountStatistic(2, Period.DAY));
  }

  private static void dumpHistory(Map<Account, List<HistoryItem>> history) {
    for (Entry<Account, List<HistoryItem>> entry : history.entrySet()) {
      System.out.println(entry.getKey());
      System.out.println(entry.getValue());
    }
  }

  private static void dumpHistory(List<HistoryItem> historyItems) {
    for (HistoryItem historyItem : historyItems) {
      System.out.println(historyItem);
    }
  }

}
