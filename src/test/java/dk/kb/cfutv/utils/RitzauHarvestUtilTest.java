package dk.kb.cfutv.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import org.testng.Assert;
import org.testng.annotations.Test;
public class RitzauHarvestUtilTest {

    /*
     * Tests:
     * Small interval: 2000-01-01T04:00 2000-01-01T05:00
     * Small interval: 2000-01-01T04:00 2000-01-01T07:00
     * Slightly bigger interval: 2000-01-01T04:00 2000-01-02T05:00  
     * Slightly bigger interval: 2000-01-01T04:00 2000-01-02T07:00
     * Pretty interval small: 2000-01-01T06:00 2000-01-02T06:00
     * Pretty interval slightly bigger: 2000-01-01T06:00 2000-01-03T06:00
     * Bigger interval: 2000-01-01T06:00 2000-01-03T05:00
     * 
     */
    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.ROOT).withZone(ZoneId.of("Europe/Copenhagen"));
    
    @Test
    public void verySmallIntervalTest() throws DateTimeParseException {

        ZonedDateTime start = ZonedDateTime.parse("2000-01-01T04:00", format);
        ZonedDateTime stop = ZonedDateTime.parse("2000-01-01T05:00", format);

        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        Assert.assertEquals(slices.size(), 1, "Should only have one timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop"); 
    }
    
    @Test
    public void smallIntervalTest() throws DateTimeParseException {
        ZonedDateTime start = ZonedDateTime.parse("2000-01-01T04:00", format);
        ZonedDateTime stop = ZonedDateTime.parse("2000-01-01T07:00", format);
        ZonedDateTime split = ZonedDateTime.parse("2000-01-01T06:00", format);

        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 2, "Should only have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split, "To time should be split");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split, "From time should be split");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop");
    }
    
    @Test
    public void slightlyBiggerIntervalTest() throws DateTimeParseException {
        ZonedDateTime start = ZonedDateTime.parse("2000-01-01T04:00", format);
        ZonedDateTime stop = ZonedDateTime.parse("2000-01-02T05:00", format);
        ZonedDateTime split = ZonedDateTime.parse("2000-01-01T06:00", format);
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 2, "Should have two timeslices");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split, "To time should be split");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split, "From time should be split");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop");
    }
    
    @Test
    public void slightlyBiggerInterval2Test() throws DateTimeParseException {
        ZonedDateTime start = ZonedDateTime.parse("2000-01-01T04:00", format);
        ZonedDateTime stop = ZonedDateTime.parse("2000-01-02T07:00", format);
        ZonedDateTime split1 = ZonedDateTime.parse("2000-01-01T06:00", format);
        ZonedDateTime split2 = ZonedDateTime.parse("2000-01-02T06:00", format);
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 3, "Should have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split1, "To time should be split1");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split1, "From time should be split1");
        Assert.assertEquals(slice.getTo(), split2, "To time should be split2");
        
        slice = slices.get(2);
        Assert.assertEquals(slice.getFrom(), split2, "From time should be split2");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop");
    }
    
    @Test
    public void smallPrettyIntervalTest() throws DateTimeParseException {
        ZonedDateTime start = ZonedDateTime.parse("2000-01-01T06:00", format);
        ZonedDateTime stop = ZonedDateTime.parse("2000-01-02T06:00", format);

        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 1, "Should have one timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop"); 
    }
    
    @Test
    public void prettyIntervalTest() throws DateTimeParseException {
        ZonedDateTime start = ZonedDateTime.parse("2000-01-01T06:00", format);
        ZonedDateTime stop = ZonedDateTime.parse("2000-01-03T06:00", format);
        ZonedDateTime split = ZonedDateTime.parse("2000-01-02T06:00", format);


        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 2, "Should have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split, "To time should be split");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split, "From time should be split");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop"); 
    }
    
    @Test
    public void slightlyBiggerInterval3Test() throws DateTimeParseException {
        ZonedDateTime start = ZonedDateTime.parse("2000-01-01T06:00", format);
        ZonedDateTime stop = ZonedDateTime.parse("2000-01-03T05:00", format);
        ZonedDateTime split = ZonedDateTime.parse("2000-01-02T06:00", format);
        
        List<HarvestTimeSlice> slices = RitzauHarvestUtil.createHarvestSlices(start, stop);
        
        Assert.assertEquals(slices.size(), 2, "Should have two timeslice");
        HarvestTimeSlice slice = slices.get(0);
        Assert.assertEquals(slice.getFrom(), start, "From time should be start");
        Assert.assertEquals(slice.getTo(), split, "To time should be split");
        
        slice = slices.get(1);
        Assert.assertEquals(slice.getFrom(), split, "From time should be split");
        Assert.assertEquals(slice.getTo(), stop, "To time should be stop"); 
    }
    
}
