package contractor.data.codex;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

public class CodexNaniteCorruption extends CodexEntryV2 implements CustomUIPanelPlugin {

	protected CustomPanelAPI panel;
	protected UIPanelAPI relatedEntries;
	protected UIPanelAPI box;
	protected CodexDialogAPI codex;

	public CodexNaniteCorruption(String id, String title, String icon) {
		super(id, title, icon);
	}

	@Override
	public void createTitleForList(TooltipMakerAPI info, float width, ListMode mode) {
		super.createTitleForList(info, width, mode);
	}

	@Override
	public boolean hasCustomDetailPanel() {
		return true;
	}

	@Override
	public CustomUIPanelPlugin getCustomPanelPlugin() {
		return this;
	}

	@Override
	public void destroyCustomDetail() {
		panel = null;
		relatedEntries = null;
		box = null;
		codex = null;
	}

	@Override
	public void createCustomDetail(CustomPanelAPI panel, UIPanelAPI relatedEntries, CodexDialogAPI codex) {
		this.panel = panel;
		this.relatedEntries = relatedEntries;
		this.codex = codex;

		Color color = Misc.getBasePlayerColor();
		Color dark = Misc.getDarkPlayerColor();
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color b = Misc.getNegativeHighlightColor();
		Color p = Misc.getPositiveHighlightColor();
		float opad = 0f;
		float pad = 3f;
		float small = 5f;

		float width = panel.getPosition().getWidth();

		float horzBoxPad = 30f;

		// the right width for a tooltip wrapped in a box to fit next to relatedEntries
		// 290 is the width of the related entries widget, but it may be null
		float tw = width - 290f - opad - horzBoxPad + 10f;

		TooltipMakerAPI text = panel.createUIElement(tw, 0, false);
		text.setParaSmallInsignia();

		text.addPara("Nanite Corruption severely impairs a ship's ability to function, and left unchecked, will result in total loss of control.", opad);
		opad = 10f;
		LabelAPI label1 = text.addPara("The corruption effect spreads at %s/%s/%s/%s base speed based on hull size, and is modified based on the ships maximum %s which can be estimated with following table:", opad, h,
				"110%", "100%", "90%", "80%", "armor value");

		text.beginTable(color, dark, Misc.getBrightPlayerColor(), 20f,
				"Armor value", 150f, "Modifier", 100f);
		text.addRow(h, "700", b, "+40%");
		text.addRow(h, "800", b, "+20%");
		text.addRow(h, "900", b, "+11%");
		text.addRow(h, "1000", g, "0%");
		text.addRow(h, "1300", p, "-23%");
		text.addRow(h, "1600", p, "-37%");
		text.addRow(h, "2000", p, "-50%");
		text.addTable("None", 0, pad);
		text.addSpacer(5f);
		text.addPara("A %s with %s armor under the effects of %s corrupter will take %s to reach maximum effect strength", opad, h,
				"destroyer", "1000", "1", "30 seconds");

		text.addPara("Affected ships suffer degraded performance such as reduced %s, %s, and %s based on severity. suffering a %s loss near the maximum strength, and ships will take random %s also based on severity.", opad, h,
				"flux dissipation", "manoeuvrability", "repair time", "33%", "Emp damage");

		text.addPara("Additionally, should the effect reach the maximum strength, the ship will be %s by the hostile firing the weapon.", opad, b,
				"taken over");

		panel.updateUIElementSizeAndMakeItProcessInput(text);

		box = panel.wrapTooltipWithBox(text);
		panel.addComponent(box).inTL(0f, 0f);
		if (relatedEntries != null) {
			panel.addComponent(relatedEntries).inTR(0f, 0f);
		}

		float height = box.getPosition().getHeight();
		if (relatedEntries != null) {
			height = Math.max(height, relatedEntries.getPosition().getHeight());
		}
		panel.getPosition().setSize(width, height);
	}

	@Override
	public Color getIconColor() {
		return Misc.getBasePlayerColor();
	}

	@Override
	public void positionChanged(PositionAPI position) {

	}

	@Override
	public void renderBelow(float alphaMult) {
	}

	@Override
	public boolean isCategory() {
		return false;
	}

	@Override
	public void render(float alphaMult) {

	}

	@Override
	public void advance(float amount) {

	}

	@Override
	public void processInput(List<InputEventAPI> events) {

	}

	@Override
	public void buttonPressed(Object buttonId) {

	}


}
