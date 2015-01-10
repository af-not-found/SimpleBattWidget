package net.afnf.and.simplebattwidget;

import java.util.Calendar;
import java.util.TimeZone;

public class BattLevelHistoryTest extends DexmakerInstrumentationTestCase {

    public void testConvertValueToUsageStr() {
        BattLevelHistory blh = new BattLevelHistory();

        assertEquals(" +0.0%/h", blh.convertValueToUsageStr(0.0f));
        assertEquals(" +1.2%/h", blh.convertValueToUsageStr(1.23f));
        assertEquals("+12.3%/h", blh.convertValueToUsageStr(12.3f));
        assertEquals(" -0.0%/h", blh.convertValueToUsageStr(-0.0f));
        assertEquals(" -1.2%/h", blh.convertValueToUsageStr(-1.23f));
        assertEquals("-12.3%/h", blh.convertValueToUsageStr(-12.3f));
    }

    public void testUpdateHistory1() {
        BattLevelHistory blh = new BattLevelHistory();

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.MONTH, 12 - 1);
        c.set(Calendar.DATE, 28);
        c.set(Calendar.HOUR, 22);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long now = c.getTimeInMillis();

        // 1つめのhistoryが更新されること
        assertEquals(true, blh.updateHistory(now, 80));
        assertEquals(80, blh.history[0]);
        assertEquals(0, blh.history[1]);
        assertEquals(80.0f, blh.calcUsage(0));

        // historyが更新されないこと
        assertEquals(false, blh.updateHistory(now, 9999));
        assertEquals(80, blh.history[0]);
        assertEquals(0, blh.history[1]);
        assertEquals(false, blh.updateHistory(now + 1, 9999));
        assertEquals(80, blh.history[0]);
        assertEquals(0, blh.history[1]);
        assertEquals(false, blh.updateHistory(now + BattLevelHistory.MSEC_1H - 1, 9999));
        assertEquals(80, blh.history[0]);
        assertEquals(0, blh.history[1]);

        // 2つめのhistoryが更新されること
        now += BattLevelHistory.MSEC_1H;
        assertEquals(true, blh.updateHistory(now, 78));
        assertEquals(78, blh.history[0]);
        assertEquals(80, blh.history[1]);
        assertEquals(0, blh.history[2]);
        assertEquals(-2.0f, blh.calcUsage(0));
        assertEquals(80.0f, blh.calcUsage(1));
        assertEquals(+0.0f, blh.calcUsage(2));

        // historyが更新されないこと
        now += BattLevelHistory.MSEC_1H - 1;
        assertEquals(false, blh.updateHistory(now, 9999));
        assertEquals(78, blh.history[0]);
        assertEquals(80, blh.history[1]);
        assertEquals(0, blh.history[2]);
        assertEquals(-2.0f, blh.calcUsage(0));
        assertEquals(80.0f, blh.calcUsage(1));
        assertEquals(+0.0f, blh.calcUsage(2));

        // 3つめのhistoryが更新されること
        now += 1;
        assertEquals(true, blh.updateHistory(now, 75));
        assertEquals(75, blh.history[0]);
        assertEquals(78, blh.history[1]);
        assertEquals(80, blh.history[2]);
        assertEquals(0, blh.history[3]);
        assertEquals(-3.0f, blh.calcUsage(0));
        assertEquals(-2.0f, blh.calcUsage(1));
        assertEquals(80.0f, blh.calcUsage(2));
        assertEquals(+0.0f, blh.calcUsage(3));

        // 4つめのhistoryが更新されること
        now += BattLevelHistory.MSEC_1H;
        assertEquals(true, blh.updateHistory(now, 74));
        assertEquals(74, blh.history[0]);
        assertEquals(75, blh.history[1]);
        assertEquals(78, blh.history[2]);
        assertEquals(80, blh.history[3]);
        assertEquals(0, blh.history[4]);
        assertEquals(-1.0f, blh.calcUsage(0));
        assertEquals(-3.0f, blh.calcUsage(1));
        assertEquals(-2.0f, blh.calcUsage(2));
        assertEquals(80.0f, blh.calcUsage(3));
        assertEquals(+0.0f, blh.calcUsage(4));

        // 5つめのhistoryが更新されること
        now += BattLevelHistory.MSEC_1H;
        assertEquals(true, blh.updateHistory(now, 76));
        assertEquals(76, blh.history[0]);
        assertEquals(74, blh.history[1]);
        assertEquals(75, blh.history[2]);
        assertEquals(78, blh.history[3]);
        assertEquals(80, blh.history[4]);
        assertEquals(0, blh.history[5]);
        assertEquals(+2.0f, blh.calcUsage(0));
        assertEquals(-1.0f, blh.calcUsage(1));
        assertEquals(-3.0f, blh.calcUsage(2));
        assertEquals(-2.0f, blh.calcUsage(3));
        assertEquals(80.0f, blh.calcUsage(4));
        assertEquals(+0.0f, blh.calcUsage(5));

        String str = blh.toString();
        assertEquals("686@76,74,75,78,80,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,", str);
        blh = new BattLevelHistory();
        blh.fromString(str);
        str = blh.toString();
        assertEquals("686@76,74,75,78,80,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,", str);
    }

    public void testUpdateHistory2() {
        BattLevelHistory blh = new BattLevelHistory();

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.MONTH, 12 - 1);
        c.set(Calendar.DATE, 28);
        c.set(Calendar.HOUR, 22);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long now = c.getTimeInMillis();

        // 1つめのhistoryが更新されること
        assertEquals(true, blh.updateHistory(now, 80));
        assertEquals(80, blh.history[0]);
        assertEquals(0, blh.history[1]);
        assertEquals(0, blh.history[2]);
        assertEquals(0, blh.history[3]);
        assertEquals(80.0f, blh.calcUsage(0));
        assertEquals(+0.0f, blh.calcUsage(1));

        // 4つめのhistoryが更新されること
        now += BattLevelHistory.MSEC_1H * 2;
        assertEquals(true, blh.updateHistory(now, 78));
        assertEquals(78, blh.history[0]);
        assertEquals(80, blh.history[1]);
        assertEquals(80, blh.history[2]);
        assertEquals(0, blh.history[3]);
        assertEquals(-2.0f, blh.calcUsage(0));
        assertEquals(+0.0f, blh.calcUsage(1));
        assertEquals(80.0f, blh.calcUsage(2));
        assertEquals(+0.0f, blh.calcUsage(3));

        // 5つめのhistoryが更新されること
        now += BattLevelHistory.MSEC_1H * 6;
        assertEquals(true, blh.updateHistory(now, 76));
        assertEquals(76, blh.history[0]);
        assertEquals(78, blh.history[1]);
        assertEquals(78, blh.history[2]);
        assertEquals(78, blh.history[3]);
        assertEquals(78, blh.history[4]);
        assertEquals(78, blh.history[5]);
        assertEquals(78, blh.history[6]);
        assertEquals(80, blh.history[7]);
        assertEquals(80, blh.history[8]);
        assertEquals(0, blh.history[9]);
        assertEquals(-2.0f, blh.calcUsage(0));
        assertEquals(+0.0f, blh.calcUsage(1));
        assertEquals(+0.0f, blh.calcUsage(2));
        assertEquals(+0.0f, blh.calcUsage(3));
        assertEquals(+0.0f, blh.calcUsage(4));
        assertEquals(+0.0f, blh.calcUsage(5));
        assertEquals(-2.0f, blh.calcUsage(6));
        assertEquals(+0.0f, blh.calcUsage(7));
        assertEquals(80.0f, blh.calcUsage(8));
        assertEquals(+0.0f, blh.calcUsage(9));

        String str = blh.toString();
        assertEquals("690@76,78,78,78,78,78,78,80,80,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,", str);
        blh = new BattLevelHistory();
        blh.fromString(str);
        str = blh.toString();
        assertEquals("690@76,78,78,78,78,78,78,80,80,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,", str);
    }
}
