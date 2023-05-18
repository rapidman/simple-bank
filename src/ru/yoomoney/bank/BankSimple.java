package ru.yoomoney.bank;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Класс представляет собой контракт на реализацию банка.
 * <p>
 * Желательно реализовать логику работы банка, с учётом его работы в многопоточной среде.
 */
public class BankSimple {

  private static final String ACCOUNT_NOT_FOUND_ERROR = "Account not found, accountId=%s";
  private final List<Account> accounts;
  private final Map<Account, List<HistoryItem>> history;
  private final ReadWriteLock historyReadWriteLock = new ReentrantReadWriteLock();

  /**
   * Конструктор
   *
   * @param accounts список счетов пользователей доступных в системе
   * @param history  история операций по счетам
   */
  public BankSimple(List<Account> accounts, Map<Account, List<HistoryItem>> history) {
    this.accounts = accounts;
    this.history = history;
  }

  /**
   * Перевести средства со счёта одного пользователя на счёт другого пользователя
   *
   * @param senderId   идентификатор счёта отправителя
   * @param receiverId идентификатор счёта получателя
   * @param amount     сумма перевода
   */
  public void transfer(long senderId, long receiverId, BigDecimal amount) {
    Account sender = getAccountById(senderId);
    Account receiver = getAccountById(receiverId);
    theadSafeApproach(amount, sender, receiver);
//    badApproach(amount, sender, receiver);
    try {
      historyReadWriteLock.writeLock().lock();
      addHistory(amount, sender, OperationType.WITHDRAW);
      addHistory(amount, receiver, OperationType.TOPUP);
    } finally {
      historyReadWriteLock.writeLock().unlock();
    }
  }

  private static void theadSafeApproach(BigDecimal amount, Account sender, Account receiver) {
    List<Account> accountsForLock = Arrays.asList(sender, receiver);
    accountsForLock.sort((o1, o2) -> o1.id > o2.id ? 1 : -1);
    synchronized (accountsForLock.get(0)){
      synchronized (accountsForLock.get(1)){
        sender.subtractAmount(amount);
        receiver.addAmount(amount);
      }
    }
  }

  private static void badApproach(BigDecimal amount, Account sender, Account receiver) {
    synchronized (sender) {
      synchronized (receiver) {
        sender.subtractAmount(amount);
        receiver.addAmount(amount);
      }
    }
  }

  public List<HistoryItem> getAccountStatistic(long accountId, Period period) {
    try {
      historyReadWriteLock.readLock().lock();
      List<HistoryItem> historyItems = history.entrySet().stream()
          .filter(entry -> entry.getKey().id == accountId)
          .flatMap(entry -> entry.getValue().stream())
          .collect(Collectors.toList());

      if (Period.DAY == period) {
        return historyItems.stream()
            .filter(BankSimple::isInDayPeriod).collect(Collectors.toList());
      } else if (Period.WEEK == period) {
        return historyItems.stream()
            .filter(BankSimple::isInWeekPeriod).collect(Collectors.toList());
      } else if (Period.MONTH == period) {
        return historyItems.stream()
            .filter(BankSimple::isInMonthPeriod).collect(Collectors.toList());
      } else {
        throw new IllegalArgumentException("Unsupported period type=" + period);
      }
    } finally {
      historyReadWriteLock.readLock().unlock();
    }
  }

  private static boolean isInDayPeriod(HistoryItem historyItem) {
    LocalDateTime start = LocalDate.now().atStartOfDay();
    return isInDateRange(historyItem, start, start.plusDays(1));
  }

  private static boolean isInWeekPeriod(HistoryItem historyItem) {
    LocalDateTime start = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay();
    return isInDateRange(historyItem, start, start.plusWeeks(1));
  }

  private static boolean isInMonthPeriod(HistoryItem historyItem) {
    LocalDateTime start = LocalDate.now()
        .with(TemporalAdjusters.firstDayOfMonth())
        .atStartOfDay();
    LocalDateTime end = start.plusMonths(1);
    return isInDateRange(historyItem, start, end);
  }

  private Account getAccountById(long accountId) {
    return accounts.stream()
        .filter(account -> account.id == accountId).findAny()
        .orElseThrow(() -> {
          throw new RuntimeException(String.format(ACCOUNT_NOT_FOUND_ERROR, accountId));
        });
  }

  private static boolean isInDateRange(HistoryItem historyItem, LocalDateTime start,
      LocalDateTime end) {
    return (start.isEqual(historyItem.date) || start.isBefore(historyItem.date)) &&
        end.isAfter(historyItem.date);
  }

  private void addHistory(BigDecimal amount, Account account, OperationType operationType) {
    history.computeIfAbsent(account, key -> new ArrayList<>())
        .add(new HistoryItem(operationType, amount, LocalDateTime.now()));
  }

  /**
   * Счёт пользователя
   */
  public static class Account {

    /**
     * Уникальный идентификатор счёта
     */
    final long id;
    /**
     * Количество средств на счёте
     */
    BigDecimal balance;

    public Account(long id, BigDecimal balance) {
      this.id = id;
      this.balance = balance;
    }

    public void addAmount(BigDecimal amount) {
      balance = balance.add(amount);
    }

    public void subtractAmount(BigDecimal amount) {
      balance = balance.subtract(amount);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Account account = (Account) o;
      return id == account.id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "Account{" +
          "id=" + id +
          ", balance=" + balance +
          '}';
    }
  }

  /**
   * Запись в истории операций пользователя
   */
  public static class HistoryItem {

    /**
     * Тип операции
     */
    final OperationType type;
    /**
     * Сумма операции
     */
    final BigDecimal amount;
    /**
     * Время проведения операции
     */
    final LocalDateTime date;

    public HistoryItem(OperationType type, BigDecimal amount, LocalDateTime date) {
      this.type = type;
      this.amount = amount;
      this.date = date;
    }

    @Override
    public String toString() {
      return "HistoryItem{" +
          "type=" + type +
          ", amount=" + amount +
          ", date=" + date +
          '}';
    }
  }

  /**
   * Тип операции в истории пользователя
   */
  public enum OperationType {
    /**
     * Снятие средств
     */
    WITHDRAW,
    /**
     * Пополнение средств
     */
    TOPUP
  }

  /**
   * Период для расчёта статистики
   */
  public enum Period {
    DAY, WEEK, MONTH
  }
}