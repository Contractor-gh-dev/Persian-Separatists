package contractor.data.codex;

import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

public class CodexCryogenic extends CodexEntryV2 implements CustomUIPanelPlugin {

	protected CustomPanelAPI panel;
	protected UIPanelAPI relatedEntries;
	protected UIPanelAPI box;
	protected CodexDialogAPI codex;

	public CodexCryogenic(String id, String title, String icon) {
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

		text.addPara("The Cryogenic Freeze status effect reduces the top speed of affected ships, depending on how severe the effect is.", opad);
		opad = 10f;
		LabelAPI label1 = text.addPara("Affected ships are slowed by %s/%s/%s at %s/%s/%s points of strength respectively; the maximum strength level is %s. " +
						"Additionally, the slowing effect weakens the longer a ship is affected, down to the minimum.", opad, h,
				"10%", "20%", "25%",
				"5", "10", "15", "30");
		label1.setHighlightColors(b, b, b, h, h, h, h);
		label1.setHighlight("10%", "20%", "25%", "5", "10", "15", "30");

		LabelAPI label2 = text.addPara("The effect has a base decay rate of %s point per second and increases the longer the effect is active, " +
						"up to a maximum of %s points per second.", opad, h,
				"1", "3");
		label2.setHighlightColors(h, h);
		label2.setHighlight("1", "3");


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
