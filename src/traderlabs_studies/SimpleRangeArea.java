package traderlabs_studies;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import com.motivewave.platform.sdk.common.ColorInfo;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.common.FontInfo;
import com.motivewave.platform.sdk.common.Inputs;
import com.motivewave.platform.sdk.common.Instrument;
import com.motivewave.platform.sdk.common.PathInfo;
import com.motivewave.platform.sdk.common.Util;
import com.motivewave.platform.sdk.draw.Figure;

/** Displays an (optionally) shaded range area. */
public class SimpleRangeArea extends Figure
{
  public SimpleRangeArea(long start, long end, double top, double bottom)
  {
    iTop = top;
    iBottom = bottom;
    this.start = start;
    this.end = end;
  }
  
  @Override
  public void draw(Graphics2D gc, DrawContext ctx)
  {
    if (!isVisible(ctx)) return;
    Line2D topLine = iTopLine;
    Line2D bottomLine = iBottomLine;
    Rectangle area = iArea;
    
    ColorInfo fill = ctx.getSettings().getColorInfo(Inputs.FILL);
    if (area != null && fill.isEnabled()) {
      gc.setColor(fill.getColor());
      gc.fill(area);
    }
    
    PathInfo topInfo = ctx.getSettings().getPath(Inputs.TOP_PATH);
    PathInfo bottomInfo = ctx.getSettings().getPath(Inputs.BOTTOM_PATH);

    if (topLine != null && topInfo.isEnabled()) {
      gc.setColor(topInfo.getColor());
      gc.setStroke(Util.getStroke(topInfo, ctx.isSelected()));
      gc.draw(topLine);
    }

    if (bottomLine != null && bottomInfo.isEnabled()) {
      gc.setColor(bottomInfo.getColor());
      gc.setStroke(Util.getStroke(bottomInfo, ctx.isSelected()));
      gc.draw(bottomLine);
    }
    
    FontInfo labelInfo = ctx.getSettings().getFont(OpeningRange.LABELS);
    if (!labelInfo.isEnabled()) return;
    
    Instrument instr = ctx.getDataContext().getInstrument();
    gc.setFont(labelInfo.getFont());
    gc.setColor(labelInfo.getColor());
    FontMetrics fm = gc.getFontMetrics();

    if (topLine != null && topInfo.isEnabled()) {
      String topLbl = "H:" + instr.format(iTop);
      Point2D p = topLine.getP2();
      if (p != null) gc.drawString(topLbl, getLX(topLbl, fm, ctx), (int)(p.getY() -3));
    }

    if (bottomLine != null && bottomInfo.isEnabled()) {
      String bottomLbl = "L:" + instr.format(iBottom);
      Point2D p = bottomLine.getP2();
      if (p != null) gc.drawString(bottomLbl, getLX(bottomLbl, fm, ctx), (int)(p.getY() -3));
    }
  }

  private int getLX(String lbl, FontMetrics fm, DrawContext ctx)
  {
    int x = iRight;
    int w = fm.stringWidth(lbl);
    x -= w;
    int gr = (int)ctx.getBounds().getMaxX();
    if (x + w > gr) {
      x = gr - w - 5; 
    }
    return x;
  }

  @Override
  public void layout(DrawContext ctx)
  {
    if (!isVisible(ctx)) return;
    Rectangle gb = ctx.getBounds();
    if (gb == null) return;
    int lx = ctx.translateTime(start);
    
    int rx = iExtendLines ? (int)ctx.getBounds().getMaxX() : ctx.translateTime(end);
    iRight = rx;
    int ty = ctx.translateValue(iTop);
    int by = ctx.translateValue(iBottom);
    int my = (ty + by)/2;

    iTopLine = Util.clipLine(lx, ty, rx, ty, gb);
    iBottomLine = Util.clipLine(lx, by, rx, by, gb);
    iArea = new Rectangle(lx, ty, rx - lx, by - ty);
  }
  
  @Override
  public boolean contains(double x, double y, DrawContext ctx)
  {
    if (!isVisible(ctx)) return false;
    PathInfo topInfo = ctx.getSettings().getPath(Inputs.TOP_PATH);
    PathInfo bottomInfo = ctx.getSettings().getPath(Inputs.BOTTOM_PATH);
    if (iTopLine != null && topInfo.isEnabled() &&  Util.distanceFromLine(x, y, iTopLine) < 6) return true;
    if (iBottomLine != null && bottomInfo.isEnabled() && Util.distanceFromLine(x, y, iBottomLine) < 6) return true;
    return false;
  }

  @Override
  public boolean isVisible(DrawContext ctx)
  {
    DataSeries series = ctx.getDataContext().getDataSeries();
    if (series.getEndTime(series.getEndIndex()) < start || series.getStartTime(series.getStartIndex()) > end) return false;
    return true;
  }
  
  public boolean isExtendLines() { return iExtendLines; }
  public void setExtendLines(boolean b) { iExtendLines=b; }
  
  public double getHigh() { return iTop; }
  public double getLow() { return iBottom; }

  public long start;
  public long end;

  private int iRight;
  private double iTop;
  private double iBottom;
  private Line2D iTopLine;
  private Line2D iBottomLine;
  private Rectangle iArea;
  private boolean iExtendLines=false;
}

