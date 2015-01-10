package net.afnf.and.simplebattwidget;

public class ConstTest extends DexmakerInstrumentationTestCase {

    public void testCalcLevel() {
        int max = 4150;
        int min = 3550;

        assertEquals(0, Const.calcLevel(max, min, min - 10, 1));
        assertEquals(0, Const.calcLevel(max, min, min - 1, 1));
        assertEquals(0, Const.calcLevel(max, min, min, 1));
        assertEquals(100, Const.calcLevel(max, min, max, 1));
        assertEquals(100, Const.calcLevel(max, min, max + 1, 1));
        assertEquals(100, Const.calcLevel(max, min, max + 10, 1));

        int d = max - min;

        int v1 = min + (int) ((double) d * 0.5);
        assertEquals(50, Const.calcLevel(max, min, v1, 1));
        assertEquals(500, Const.calcLevel(max, min, v1, 10));
        assertEquals(5000, Const.calcLevel(max, min, v1, 100));

        int v2 = min + (int) ((double) d * 0.75);
        assertEquals(75, Const.calcLevel(max, min, v2, 1));

        int v3 = min + (int) ((double) d * 0.25);
        assertEquals(25, Const.calcLevel(max, min, v3, 1));

        assertEquals(3, Const.calcLevel(max, min, 3568, 1));
        assertEquals(47, Const.calcLevel(max, min, 3834, 1));
        assertEquals(87, Const.calcLevel(max, min, 4076, 1));
        assertEquals(876, Const.calcLevel(max, min, 4076, 10));
        assertEquals(8766, Const.calcLevel(max, min, 4076, 100));
    }
}
