package ru.yoomoney.bank.concurrency;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import ru.yoomoney.bank.BankSimple;

/**
 * @author timur
 * @since 18.05.2023
 */
public class Worker implements Runnable{
  private final CountDownLatch countDownLatch;
  private final BankSimple bankSimple;
  private final long senderId;
  private final long receiverId;
  private final BigDecimal amount;

  public Worker(CountDownLatch countDownLatch, BankSimple bankSimple, long senderId,
      long receiverId, BigDecimal amount) {
    this.countDownLatch = countDownLatch;
    this.bankSimple = bankSimple;
    this.senderId = senderId;
    this.receiverId = receiverId;
    this.amount = amount;
  }

  @Override
  public void run() {
    System.out.println("Transfer started");
    try {
      for (int i = 0; i < 10; i++) {
        Thread.sleep(100);
        bankSimple.transfer(senderId, receiverId, amount);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    countDownLatch.countDown();
    System.out.println("Transfer finished");
  }
}
