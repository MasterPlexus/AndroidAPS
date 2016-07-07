package info.nightscout.androidaps.plugins.DanaR.comm;

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.plugins.DanaR.DanaRFragment;

public class MsgStatusBolusExtended extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgStatusBolusExtended.class);

    public MsgStatusBolusExtended() {
        SetCommand(0x0207);
    }

    public void handleMessage(byte[] bytes) {
        boolean isExtendedInProgress = intFromBuff(bytes, 0, 1) == 1;
        int extendedBolusHalfHours = intFromBuff(bytes, 1, 1);
        int extendedBolusMinutes = extendedBolusHalfHours * 30;

        double extendedBolusAmount = intFromBuff(bytes, 2, 2) / 100d;
        int extendedBolusSoFarInSecs = intFromBuff(bytes, 4, 3);

        int extendedBolusSoFarInMinutes = extendedBolusSoFarInSecs / 60;
        double extendedBolusAbsoluteRate = isExtendedInProgress ? extendedBolusAmount / extendedBolusMinutes * 60 : 0d;
        Date extendedBolusStart = isExtendedInProgress ? getDateFromSecAgo(extendedBolusSoFarInSecs) : new Date(0);
        int extendedBolusRemainingMinutes = extendedBolusMinutes - extendedBolusSoFarInMinutes;

        DanaRFragment.getDanaRPump().isExtendedInProgress = isExtendedInProgress;
        DanaRFragment.getDanaRPump().extendedBolusMinutes = extendedBolusMinutes;
        DanaRFragment.getDanaRPump().extendedBolusAmount = extendedBolusAmount;
        DanaRFragment.getDanaRPump().extendedBolusSoFarInMinutes = extendedBolusSoFarInMinutes;
        DanaRFragment.getDanaRPump().extendedBolusAbsoluteRate = extendedBolusAbsoluteRate;
        DanaRFragment.getDanaRPump().extendedBolusStart = extendedBolusStart;
        DanaRFragment.getDanaRPump().extendedBolusRemainingMinutes = extendedBolusRemainingMinutes;

        if (Config.logDanaMessageDetail) {
            log.debug("Is extended bolus running: " + isExtendedInProgress);
            log.debug("Extended bolus min: " + extendedBolusMinutes);
            log.debug("Extended bolus amount: " + extendedBolusAmount);
            log.debug("Extended bolus so far in minutes: " + extendedBolusSoFarInMinutes);
            log.debug("Extended bolus absolute rate: " + extendedBolusAbsoluteRate);
            log.debug("Extended bolus start: " + extendedBolusStart);
            log.debug("Extended bolus remaining minutes: " + extendedBolusRemainingMinutes);
        }
    }

    @NonNull
    private Date getDateFromSecAgo(int tempBasalAgoSecs) {
        return new Date((long) (Math.ceil(new Date().getTime() / 1000d) - tempBasalAgoSecs) * 1000);
    }
}
