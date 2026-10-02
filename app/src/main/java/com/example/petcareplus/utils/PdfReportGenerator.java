package com.example.petcareplus.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfReportGenerator {

    private static final int W = 595, H = 842, M = 40, CW = W - 2 * M;
    private static final int GREEN = 0xFF2F6B52, DARK = 0xFF152922, GREY = 0xFF6B7B73,
            LIGHT = 0xFFEAF3EE, LINE = 0xFFD9E3DD;

    public static class Row {
        public final String title, date, detail;
        public Row(String title, String date, String detail) {
            this.title = title; this.date = date; this.detail = detail;
        }
    }

    public static class Section {
        public final String title;
        public final int color;
        public final List<Row> rows = new ArrayList<>();
        public Section(String title, int color) { this.title = title; this.color = color; }
    }

    // ---------- state ----------
    private final PdfDocument doc = new PdfDocument();
    private PdfDocument.Page page;
    private Canvas c;
    private int y, pageNo = 0;

    private final Paint pTitle = paint(20, GREEN, true);
    private final Paint pWhite = paint(20, 0xFFFFFFFF, true);
    private final Paint pWhiteSmall = paint(11, 0xFFFFFFFF, false);
    private final Paint pH = paint(13, DARK, true);
    private final Paint pBody = paint(11, DARK, false);
    private final Paint pBold = paint(11, DARK, true);
    private final Paint pGrey = paint(10, GREY, false);
    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLine = new Paint();

    private static Paint paint(float size, int color, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return p;
    }

    public static File generate(Context ctx, String petName, String petType, String petBreed,
                                String petAge, String petWeight, String periodLabel,
                                String summaryLine, List<Section> sections, List<Row> upcoming)
            throws IOException {
        return new PdfReportGenerator().build(ctx, petName, petType, petBreed, petAge, petWeight,
                periodLabel, summaryLine, sections, upcoming);
    }

    private File build(Context ctx, String petName, String petType, String petBreed,
                       String petAge, String petWeight, String periodLabel,
                       String summaryLine, List<Section> sections, List<Row> upcoming)
            throws IOException {

        pLine.setColor(LINE);
        pLine.setStrokeWidth(1f);

        newPage();

        // ---- header band ----
        pFill.setColor(GREEN);
        c.drawRect(0, 0, W, 78, pFill);
        c.drawText("PetCare+ Health Report", M, 38, pWhite);
        c.drawText(periodLabel + "   |   Generated "
                        + new SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(new Date()),
                M, 60, pWhiteSmall);
        y = 98;

        // ---- pet card ----
        pFill.setColor(LIGHT);
        c.drawRoundRect(new RectF(M, y, W - M, y + 74), 12, 12, pFill);
        c.drawText(petName, M + 14, y + 24, pTitle);
        c.drawText("Type: " + petType + "     Breed: " + petBreed, M + 14, y + 44, pBody);
        c.drawText("Age: " + petAge + "     Weight: " + petWeight + " kg", M + 14, y + 61, pBody);
        y += 74 + 18;

        // ---- summary ----
        c.drawText("Summary", M, y, pH);
        y += 16;
        for (String line : wrap(summaryLine, pBody, CW)) {
            c.drawText(line, M, y, pBody);
            y += 15;
        }
        y += 10;

        // ---- sections ----
        for (Section s : sections) drawSection(s.title + " (" + s.rows.size() + ")", s.color, s.rows,
                "No records in this period.");

        if (upcoming != null && !upcoming.isEmpty()) {
            drawSection("Upcoming (" + upcoming.size() + ")", 0xFFE07A3E, upcoming, "");
        }

        doc.finishPage(page);

        File dir = new File(ctx.getCacheDir(), "reports");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, "PetCarePlus_Report_" + petName.replaceAll("[^a-zA-Z0-9]", "_")
                + "_" + System.currentTimeMillis() + ".pdf");

        FileOutputStream fos = new FileOutputStream(out);
        try {
            doc.writeTo(fos);
        } finally {
            doc.close();
            fos.close();
        }
        return out;
    }

    private void drawSection(String heading, int color, List<Row> rows, String emptyText) {

        ensure(60);

        pFill.setColor(LIGHT);
        c.drawRoundRect(new RectF(M, y, W - M, y + 26), 8, 8, pFill);
        pFill.setColor(color);
        c.drawRoundRect(new RectF(M, y, M + 6, y + 26), 3, 3, pFill);
        c.drawText(heading, M + 16, y + 18, pH);
        y += 26 + 14;

        if (rows.isEmpty()) {
            if (!emptyText.isEmpty()) {
                c.drawText(emptyText, M + 8, y, pGrey);
                y += 20;
            }
            y += 6;
            return;
        }

        final int dateW = 82;

        for (Row r : rows) {

            List<String> titleLines = wrap(r.title, pBold, CW - dateW - 10);
            List<String> detailLines = (r.detail == null || r.detail.isEmpty())
                    ? new ArrayList<>() : wrap(r.detail, pGrey, CW - dateW - 10);

            int needed = titleLines.size() * 15 + detailLines.size() * 13 + 12;
            ensure(needed);

            int rowTop = y;
            c.drawText(r.date == null ? "-" : r.date, M + 8, y, pGrey);

            for (String l : titleLines) {
                c.drawText(l, M + dateW + 8, y, pBold);
                y += 15;
            }
            for (String l : detailLines) {
                c.drawText(l, M + dateW + 8, y, pGrey);
                y += 13;
            }
            y += 6;
            c.drawLine(M, y - 2, W - M, y - 2, pLine);
            y += 12;
        }
        y += 6;
    }

    private void ensure(int needed) {
        if (y + needed > H - 50) {
            newPage();
            y = M;
        }
    }

    private void newPage() {
        if (page != null) doc.finishPage(page);
        pageNo++;
        page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
        c = page.getCanvas();
        c.drawLine(M, H - 34, W - M, H - 34, pLine);
        c.drawText("PetCare+  -  Healthier Pets, Happier Lives", M, H - 18, pGrey);
        c.drawText("Page " + pageNo, W - M - 40, H - 18, pGrey);
        y = M;
    }

    private static List<String> wrap(String text, Paint p, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (p.measureText(test) > maxWidth && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}