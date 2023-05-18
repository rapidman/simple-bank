package ru.yoomoney.bank;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import ru.yoomoney.bank.BankSimple.Account;
import ru.yoomoney.bank.BankSimple.HistoryItem;
import ru.yoomoney.bank.BankSimple.Period;
import ru.yoomoney.bank.concurrency.Worker;

/**
 * @author timur
 * @since 18.05.2023
 */
class BankSimpleTest {

  private static final int TOTAL_ACCOUNTS_COUNT = 100;

  public static void main(String[] args) {
//    testSingleThread();
    try {
      testConcurrency();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void testSingleThread() {
    List<Account> accounts = getAccounts();
    Map<Account, List<HistoryItem>> history = new HashMap<>();
    BankSimple bankSimple = new BankSimple(accounts, history);
    bankSimple.transfer(1, 2, new BigDecimal(1_000));
    bankSimple.transfer(1, 2, new BigDecimal(1_000));
    dumpHistory(history);
    System.out.println("==================");
    dumpHistory(bankSimple.getAccountStatistic(1, Period.DAY));
    dumpHistory(bankSimple.getAccountStatistic(2, Period.DAY));
  }

  private static void testConcurrency() throws Exception {
    int workersCount = 100;
    CountDownLatch countDownLatch = new CountDownLatch(workersCount);
    Map<Account, List<HistoryItem>> history = new HashMap<>();
    List<Account> accounts = getAccounts();
    BankSimple bankSimple = new BankSimple(accounts, history);

    long senderId = 1;
    long receiverId = 2;
    BigDecimal amount = new BigDecimal(10);

    startWorkers(workersCount / 2, countDownLatch, bankSimple, senderId, receiverId, amount);
    startWorkers(workersCount / 2, countDownLatch, bankSimple, receiverId, senderId, amount);
    countDownLatch.await();
    System.out.println("Latch released");
  }

  private static void startWorkers(int workersCount, CountDownLatch countDownLatch,
      BankSimple bankSimple, long senderId, long receiverId, BigDecimal amount) {
    List<Thread> workers = Stream
        .generate(
            () -> new Thread(new Worker(countDownLatch, bankSimple, senderId, receiverId, amount)))
        .limit(workersCount)
        .collect(toList());

    workers.forEach(Thread::start);
  }

  private static List<Account> getAccounts() {
    TestAccountProvider accountProvider = new TestAccountProvider();
    List<Account> accounts = accountProvider.createAccounts(TOTAL_ACCOUNTS_COUNT);
    return accounts;
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
