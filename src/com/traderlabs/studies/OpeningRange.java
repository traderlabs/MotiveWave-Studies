package com.traderlabs.studies;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Enums.BarSizeType;
import com.motivewave.platform.sdk.common.Inputs;
import com.motivewave.platform.sdk.common.Instrument;
import com.motivewave.platform.sdk.common.desc.BarSizeDescriptor;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.FontDescriptor;
import com.motivewave.platform.sdk.common.desc.IndicatorDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.SettingGroup;
import com.motivewave.platform.sdk.common.desc.SettingTab;
import com.motivewave.platform.sdk.common.desc.SettingsDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.study.RuntimeDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

/** Opening Range.  Displays the range of opening period as a shaded area and/or top and bottom lines
    By default this study will show the 30 minutes opening range. */
@StudyHeader(
    namespace="com.traderlabs",
    id="OPENING_RANGE",
    rb="com.traderlabs.nls.strings",
    name="Opening Range",
    label="Opening Range",
    desc="Show Opening Range for a day",
    menu="MENU_TRADERLABS",
    overlay=true,
    studyOverlay=true,
    supportsBarUpdates=false,
    helpLink="")
public class OpeningRange extends Study
{
  final static String SHOW_ALL = "showAll";
  final static String EXTEND_RIGHT = "extendRight";
  final static String LABELS = "labels";
  final static String HIGH_IND = "highInd";
  final static String LOW_IND = "lowInd";
  final static String DURATION = "duration";

  enum Values { OPEN_HIGH, OPEN_LOW };

  @Override
  public void initialize(Defaults defaults)
  {
    SettingsDescriptor sd = new SettingsDescriptor();
    SettingTab tab = new SettingTab(get("TAB_GENERAL"));
    sd.addTab(tab);
    setSettingsDescriptor(sd);
    // Required invisible input to declare a 1 day barsize
    sd.addInvisibleSetting(new BarSizeDescriptor(DURATION, get("LBL_DURATION"), BarSize.getBarSize(BarSizeType.LINEAR, 1440)));

    SettingGroup inputs = new SettingGroup(get("LBL_INPUTS"));
    inputs.addRow(new BarSizeDescriptor(Inputs.BARSIZE, get("LBL_TIMEFRAME"), BarSize.getBarSize(BarSizeType.LINEAR, 30)));
    inputs.addRow(new BooleanDescriptor(SHOW_ALL, get("LBL_SHOW_ALL"), true));
    inputs.addRow(new BooleanDescriptor(EXTEND_RIGHT, get("LBL_EXTEND_RIGHT"), true));
    tab.addGroup(inputs);

    SettingGroup colors = new SettingGroup(get("LBL_DISPLAY"));
    colors.addRow(new PathDescriptor(Inputs.TOP_PATH, get("LBL_TOP_LINE"), defaults.getLineColor(), 2.0f, null, true, false, false, true));
    colors.addRow(new PathDescriptor(Inputs.BOTTOM_PATH, get("LBL_BOTTOM_LINE"), defaults.getLineColor(), 2.0f, null, true, false, false, true));
    colors.addRow(new ColorDescriptor(Inputs.FILL, get("LBL_FILL"), defaults.getFillColor(), false, true));
    colors.addRow(new FontDescriptor(LABELS, get("LBL_LABELS"), defaults.getFont(), defaults.getTextColor(), true, false, true));
    colors.addRow(new IndicatorDescriptor(HIGH_IND, get("LBL_HIGH_IND"), defaults.getBlue(), null, false, true, true));
    colors.addRow(new IndicatorDescriptor(LOW_IND, get("LBL_LOW_IND"), defaults.getRed(), null, false, true, true));
    tab.addGroup(colors);

    RuntimeDescriptor desc = new RuntimeDescriptor();
    desc.setLabelSettings(Inputs.BARSIZE, String.valueOf(Values.OPEN_HIGH), String.valueOf(Values.OPEN_LOW));
    desc.declareIndicator(Values.OPEN_HIGH, HIGH_IND);
    desc.declareIndicator(Values.OPEN_LOW, LOW_IND);
    setRuntimeDescriptor(desc);
    desc.exportValue(new ValueDescriptor(Values.OPEN_HIGH, get("OPEN HIGH"), 
            new String[] {Inputs.PERIOD}));
    desc.exportValue(new ValueDescriptor(Values.OPEN_LOW, get("OPEN LOW"), 
            new String[] {Inputs.PERIOD}));
    // MotiveWave will automatically draw a path using the path settings
	// (described above with the key 'Inputs.LINE')  In this case 
	// it will use the values generated in the 'calculate' method
	// and stored in the data series using the key 'Values.MA'
    desc.declarePath(Values.OPEN_HIGH, Inputs.TOP_PATH);
    desc.declarePath(Values.OPEN_LOW, Inputs.BOTTOM_PATH);
  }

  /** Override this method to clear any internal state that may be kept within the study.
  This method is called when the settings have been updated, the user chooses a new bar size or
  when additional historical data is loaded into the series */
  @Override
  public void clearState()
  {
    super.clearState();
    iOpenRangeSets.clear();
  }

  @Override
  public void onBarClose(DataContext ctx) { calculateValues(ctx); }

  @Override
  protected void calculateValues(DataContext ctx)
  {
    BarSize openBarSize = getSettings().getBarSize(Inputs.BARSIZE);
    BarSize dayBarSize = BarSize.getBarSize(BarSizeType.LINEAR, 1440);
    
    DataSeries daySeries = ctx.getDataSeries(dayBarSize);    
    DataSeries openSeries = ctx.getDataSeries(openBarSize);
    
    boolean showPrevOpeningRanges = getSettings().getBoolean(SHOW_ALL, true);

    int start = 1;
    int j = 1;

    if (!showPrevOpeningRanges) {
      start = daySeries.size() - 1;
      iOpenRangeSets.clear();
      clearFigures();
    }

    Instrument instr = daySeries.getInstrument();
   
    for(int i = start; i < daySeries.size(); i++) {
      long time = daySeries.getStartTime(i);
      
      time = instr.getStartOfDay(time, true);
      if (iOpenRangeSets.containsKey(time)) {
    	  continue;
      
      }
      
      long innerTime = openSeries.getStartTime(j);
      
      while(innerTime < time && j < openSeries.size()) {
    	  j++;
    	  innerTime = openSeries.getStartTime(j);
      }
     
      SimpleRangeArea area = new SimpleRangeArea(time, instr.getEndOfDay(time, true), openSeries.getHigh(j), openSeries.getLow(j));
      area.setExtendLines(false);
      iOpenRangeSets.put(time, area);
      addFigure(area);
    }
    
    SimpleRangeArea area = null;
    for(SimpleRangeArea _area : iOpenRangeSets.values()) {
      if (area == null) area = _area;
      else {
        if (_area.start > area.start) area = _area;
      }
    }

    if (area == null) return;
    
    DataSeries series = ctx.getDataSeries();
    series.setDouble(Values.OPEN_HIGH, area.getHigh());
    series.setDouble(Values.OPEN_LOW, area.getLow());

  }
  protected Map<Long, SimpleRangeArea> iOpenRangeSets = Collections.synchronizedMap(new HashMap<Long, SimpleRangeArea>());

}
