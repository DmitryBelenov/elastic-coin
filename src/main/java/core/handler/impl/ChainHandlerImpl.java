package core.handler.impl;

import core.ElasticCoinManager;
import core.element.impl.BlockImpl;
import core.element.interfaces.Block;
import core.handler.interfaces.ChainHandler;
import org.codehaus.jackson.map.ObjectMapper;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChainHandlerImpl implements ChainHandler {

    private ElasticCoinManager ecm;
    private String coinHome;

    public ChainHandlerImpl(ElasticCoinManager ecm) throws Exception {
        this.ecm = ecm;
        coinHome = this.ecm.getProps().getProperty("elcoin.home");
    }

    @Override
    public boolean checkChainIntegrity() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS");

        File[] home = (new File(coinHome)).listFiles();
        if (home != null) {
          if (home.length > 1) {
            Map<Date, Block> chainMap = new TreeMap<>();
            for (File block : home) {
                Block current = mapper.readValue(block, BlockImpl.class);
                Date date = formatter.parse(current.getCreateDate());

                chainMap.put(date, current);
            }

            List<Block> blocks = new ArrayList<>(chainMap.values());

            for (int i = blocks.size() - 1; i >= 0; i--) {
                if (i == 0) {
                    Block genesis = blocks.get(i);
                    if (!genesis.getPreviousHash().equals("genesis_block")) return false;
                    break;
                }

                Block cur = blocks.get(i);
                Block pre = blocks.get(i - 1);

                if (!cur.getPreviousHash().equals(pre.getHash())) return false;
            }
        }
        } else {
            ecm.getLogger().log("Unable to read coin home for check chain integrity, cause it's empty");
            return false;
        }
        return true;
    }
}
