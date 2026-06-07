package com.pelletsfactory.stock_manager.desktop.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.pelletsfactory.stock_manager.common.dto.response.EncomendaFornecedorDetailsDTO;
import com.pelletsfactory.stock_manager.common.dto.response.ItemEncomendaFornecedorResponseDTO;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PurchaseOrderPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color ACCENT = new Color(59, 130, 246);
    private static final Color MUTED  = new Color(107, 114, 128);
    private static final Color FG     = new Color(17, 24, 39);
    private static final Color ROW_ALT = new Color(249, 250, 251);

    public void generate(EncomendaFornecedorDetailsDTO order, int poSeq, OutputStream out) throws Exception {
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont   = new Font(Font.HELVETICA, 22, Font.BOLD,   FG);
        Font poNumFont   = new Font(Font.HELVETICA, 13, Font.BOLD,   ACCENT);
        Font sectionFont = new Font(Font.HELVETICA, 10, Font.BOLD,   ACCENT);
        Font labelFont   = new Font(Font.HELVETICA, 10, Font.BOLD,   MUTED);
        Font valueFont   = new Font(Font.HELVETICA, 10, Font.NORMAL, FG);
        Font headerFont  = new Font(Font.HELVETICA, 9,  Font.BOLD,   Color.WHITE);
        Font cellFont    = new Font(Font.HELVETICA, 9,  Font.NORMAL, FG);
        Font boldFont    = new Font(Font.HELVETICA, 11, Font.BOLD,   FG);

        String symbol = "EUR".equals(order.moedaCodigo()) ? "€" : (order.moedaCodigo() != null ? order.moedaCodigo() + " " : "€");

        // ── Title ───────────────────────────────────────────────────────────
        Paragraph title = new Paragraph("PURCHASE ORDER", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph poNum = new Paragraph(String.format("PO-%03d", poSeq), poNumFont);
        poNum.setSpacingAfter(20);
        doc.add(poNum);

        // ── Order info (2-column grid) ───────────────────────────────────────
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1.2f, 2f, 1.2f, 2f});
        infoTable.setSpacingAfter(20);

        addInfoPair(infoTable, "Supplier",    valorOuTraco(order.fornecedorNome()), labelFont, valueFont);
        addInfoPair(infoTable, "Order Date",  order.data() != null ? order.data().format(DATE_FMT) : "—", labelFont, valueFont);
        addInfoPair(infoTable, "Status",      order.estado() != null ? order.estado().name() : "—", labelFont, valueFont);
        addInfoPair(infoTable, "Currency",    valorOuTraco(order.moedaCodigo()), labelFont, valueFont);
        doc.add(infoTable);

        // ── Items section ────────────────────────────────────────────────────
        Paragraph itemsTitle = new Paragraph("ORDER ITEMS", sectionFont);
        itemsTitle.setSpacingAfter(8);
        doc.add(itemsTitle);

        PdfPTable itemsTable = new PdfPTable(6);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{3f, 1f, 1.2f, 1.5f, 1f, 1.5f});
        itemsTable.setSpacingAfter(20);

        for (String h : new String[]{"Raw Material", "Unit", "Qty", "Unit Price", "VAT %", "Total"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(ACCENT);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            itemsTable.addCell(cell);
        }

        if (order.itens() != null && !order.itens().isEmpty()) {
            boolean alt = false;
            for (ItemEncomendaFornecedorResponseDTO item : order.itens()) {
                Color bg = alt ? ROW_ALT : Color.WHITE;
                alt = !alt;
                double qty   = item.quantidade()      != null ? item.quantidade()      : 0;
                double price = item.precoUnitarioNet() != null ? item.precoUnitarioNet() : 0;
                double total = qty * price;

                addItemCell(itemsTable, valorOuTraco(item.materiaPrimaNome()), cellFont, bg, Element.ALIGN_LEFT);
                addItemCell(itemsTable, valorOuTraco(item.unidade()),          cellFont, bg, Element.ALIGN_CENTER);
                addItemCell(itemsTable, item.quantidade() != null ? String.format("%.2f", qty)            : "—", cellFont, bg, Element.ALIGN_RIGHT);
                addItemCell(itemsTable, item.precoUnitarioNet() != null ? String.format("%s%.2f", symbol, price) : "—", cellFont, bg, Element.ALIGN_RIGHT);
                addItemCell(itemsTable, item.taxaIva() != null ? String.format("%.0f%%", item.taxaIva())  : "—", cellFont, bg, Element.ALIGN_CENTER);
                addItemCell(itemsTable, String.format("%s%.2f", symbol, total), cellFont, bg, Element.ALIGN_RIGHT);
            }
        } else {
            PdfPCell empty = new PdfPCell(new Phrase("No items", cellFont));
            empty.setColspan(6);
            empty.setPadding(8);
            empty.setBorderColor(new Color(229, 231, 235));
            itemsTable.addCell(empty);
        }
        doc.add(itemsTable);

        // ── Totals ───────────────────────────────────────────────────────────
        Paragraph totTitle = new Paragraph("ORDER TOTAL", sectionFont);
        totTitle.setSpacingAfter(8);
        doc.add(totTitle);

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(45);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{2f, 1.5f});
        totals.setSpacingAfter(20);

        addTotalRow(totals, "Subtotal",    order.totalLiquido(), symbol, labelFont, valueFont, false);
        addTotalRow(totals, "VAT",         order.totalIva(),     symbol, labelFont, valueFont, false);
        addTotalRow(totals, "GRAND TOTAL", order.totalFinal(),   symbol, labelFont, boldFont,  true);
        doc.add(totals);

        doc.close();
    }

    private void addInfoPair(PdfPTable t, String label, String value, Font lf, Font vf) {
        PdfPCell k = new PdfPCell(new Phrase(label, lf));
        k.setBorder(Rectangle.NO_BORDER); k.setPadding(5);
        t.addCell(k);
        PdfPCell v = new PdfPCell(new Phrase(value, vf));
        v.setBorder(Rectangle.NO_BORDER); v.setPadding(5);
        t.addCell(v);
    }

    private void addItemCell(PdfPTable t, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setBorderWidth(0.5f);
        t.addCell(cell);
    }

    private void addTotalRow(PdfPTable t, String label, Double amount, String symbol, Font lf, Font vf, boolean highlight) {
        Color bg = highlight ? new Color(239, 246, 255) : Color.WHITE;
        PdfPCell k = new PdfPCell(new Phrase(label, lf));
        k.setBorder(Rectangle.NO_BORDER); k.setPadding(6); k.setBackgroundColor(bg);
        t.addCell(k);
        String val = amount != null ? String.format("%s%.2f", symbol, amount) : "—";
        PdfPCell v = new PdfPCell(new Phrase(val, vf));
        v.setBorder(Rectangle.NO_BORDER); v.setPadding(6);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT); v.setBackgroundColor(bg);
        t.addCell(v);
    }

    private String valorOuTraco(String s) { return s != null && !s.isBlank() ? s : "—"; }
}
