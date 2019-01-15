package org.tron.common.logsfilter.trigger;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.tron.common.runtime.vm.program.InternalTransaction;

public class TransactionLogTrigger extends Trigger {

  @Override
  public void setTimeStamp(long ts) {
    super.timeStamp = ts;
  }

  @Getter
  @Setter
  private String transactionId;

  @Getter
  @Setter
  private String blockHash;

  @Getter
  @Setter
  private long blockNumber = -1;

  @Getter
  @Setter
  private long energyUsage;

  @Getter
  @Setter
  private long energyFee;

  @Getter
  @Setter
  private long originEnergyUsage;

  @Getter
  @Setter
  private long energyUsageTotal;

  @Getter
  @Setter
  private long netUsage;

  @Getter
  @Setter
  private long netFee;

  //contract
  @Getter
  @Setter
  private String result;

  @Getter
  @Setter
  private String contractAddress;

  @Getter
  @Setter
  private String contractType;

  @Getter
  @Setter
  private long feeLimit;

  @Getter
  @Setter
  private long callValue;

  @Getter
  @Setter
  private String contractResult;

  //internal transaction
  @Getter
  @Setter
  private List<InternalTransactionPojo> internalTrananctionList;

  @Getter
  @Setter
  private String fromAddress;

  @Getter
  @Setter
  private String toAddress;

  @Getter
  @Setter
  private String AssetName;

  @Getter
  @Setter
  private long AssetAmount;

  public TransactionLogTrigger() {
    setTriggerName(Trigger.TRANSACTION_TRIGGER_NAME);
    internalTrananctionList = new ArrayList<>();
  }

  public void setInternalTransactionPojoList(List<InternalTransaction> internalTransactionList) {
    internalTransactionList.forEach(internalTransaction -> {
      this.internalTrananctionList.add(new InternalTransactionPojo(internalTransaction));
    });

  }
}
