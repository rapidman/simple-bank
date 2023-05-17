package ru.yoomoney.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Класс представляет собой контракт на реализацию банка.
 * <p>
 * Желательно реализовать логику работы банка, с учётом его работы в многопоточной среде.
 */
class BankSimple {

  private static final String ACCOUNT_NOT_FOUND_ERROR = "Account not found, accountId=%s";
  private final List<Account> accounts;
  private final Map<Account, List<HistoryItem>> history;

  /**
   * Конструктор
   *
   * @param accounts список счетов пользователей доступных в системе
   * @param history  история операций по счетам
   */
  public BankSimple(List<Account> accounts, Map<Account, List<HistoryItem>> history) {
    // TODO
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
    synchronized (sender) {
      synchronized (receiver) {
        sender.subtractAmount(amount);
        receiver.addAmount(amount);
      }
    }
  }

  private Account getAccountById(long accountId) {
    return accounts.stream()
        .filter(account -> account.id == accountId).findAny()
        .orElseThrow(() -> {
          throw new RuntimeException(String.format(ACCOUNT_NOT_FOUND_ERROR, accountId));
        });
  }


  /**
   * Счёт пользователя
   */
  public static class Account {

    /**
     * Уникальный идентификатор счёта
     */
    long id;
    /**
     * Количество средств на счёте
     */
    BigDecimal balance;

    public void addAmount(BigDecimal amount) {
      balance = balance.add(amount);
    }

    public void subtractAmount(BigDecimal amount) {
      balance = balance.subtract(amount);
    }
  }

  /**
   * Запись в истории операций пользователя
   */
  public static class HistoryItem {

    /**
     * Тип операции
     */
    OperationType type;
    /**
     * Сумма операции
     */
    BigDecimal amount;
    /**
     * Время проведения операции
     */
    LocalDateTime date;

    public HistoryItem(OperationType type, BigDecimal amount, LocalDateTime date) {
      this.type = type;
      this.amount = amount;
      this.date = date;
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