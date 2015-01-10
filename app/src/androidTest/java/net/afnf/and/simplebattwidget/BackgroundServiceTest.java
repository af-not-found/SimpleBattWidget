package net.afnf.and.simplebattwidget;

public class BackgroundServiceTest extends DexmakerInstrumentationTestCase {

    public void testAverageVoltage1() {
        BackgroundService service = new BackgroundService();
        assertEquals(3600, service.averageVoltage(3600));
        assertEquals(3600, service.averageVoltage(3600));
        assertEquals(3600, service.averageVoltage(3600));
        assertEquals(3600, service.averageVoltage(3600));
        assertEquals(3672, service.averageVoltage(3800));
        assertEquals(3744, service.averageVoltage(3800));
    }

    public void testAverageVoltage2() {
        BackgroundService service = new BackgroundService();
        assertEquals(3600, service.averageVoltage(3600));
        assertEquals(3600, service.averageVoltage(3600));
        assertEquals(3672, service.averageVoltage(3800));
        assertEquals(3744, service.averageVoltage(3800));
        assertEquals(3816, service.averageVoltage(3900));
    }
}
