package org.tron.core.db;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.DeferredTransactionCapsule;
import java.util.List;
import org.tron.core.exception.BadItemException;

@Slf4j(topic = "DB")
@Component
public class DeferredTransactionStore extends TronStoreWithRevoking<DeferredTransactionCapsule>  {
    @Autowired
    private DeferredTransactionStore(@Value("deferred_transaction") String dbName) {
        super(dbName);
    }

    public void put(DeferredTransactionCapsule deferredTransactionCapsule){
        super.put(deferredTransactionCapsule.getKey(), deferredTransactionCapsule);
    }

    public List<DeferredTransactionCapsule> getScheduledTransactions (long time){
        return revokingDB.getValuesPrevious(Longs.toByteArray(time)).stream()
            .map(DeferredTransactionCapsule::new)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public void removeDeferredTransactionById(DeferredTransactionCapsule deferredTransactionCapsule) {
        revokingDB.delete(deferredTransactionCapsule.getKey());
    }

    public DeferredTransactionCapsule getByTransactionId(ByteString transactionId){
        DeferredTransactionCapsule deferredTransactionCapsule = null;

        try{
            byte[] key = transactionId.toByteArray();
            byte[] value = revokingDB.getUnchecked(key);
            if (ArrayUtils.isEmpty(value)) {
                return null;
            }

            deferredTransactionCapsule = new DeferredTransactionCapsule(value);
        }
        catch (Exception e){
            logger.error("{}", e);
        }

        return deferredTransactionCapsule;
    }
}
