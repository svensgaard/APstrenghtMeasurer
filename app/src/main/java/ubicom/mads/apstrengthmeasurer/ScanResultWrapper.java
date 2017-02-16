package ubicom.mads.apstrengthmeasurer;

/**
 * Created by Mads on 15-02-2017.
 */
public class ScanResultWrapper {
    String APaddress;
    int level;
    String room;
    Long timestampMilis;
    String deviceAddress;

    public ScanResultWrapper(String APaddress, int level, String room, String deviceAddress) {
        this.APaddress = APaddress;
        this.level = level;
        this.room = room;
        timestampMilis = System.currentTimeMillis();
        this.deviceAddress = deviceAddress;
    }
}
