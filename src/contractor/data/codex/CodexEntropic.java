package contractor.data.codex;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

public class CodexEntropic extends CodexEntryV2 implements CustomUIPanelPlugin {
	protected CustomPanelAPI panel;
	protected UIPanelAPI relatedEntries;
	protected UIPanelAPI box;
	protected CodexDialogAPI codex;

	public CodexEntropic(String id, String title, String icon) {
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

		text.addPara("The Entropic Instability status effect causes ships equipment to experience heavy stress.", opad);
		opad = 8f;
		text.addPara("Affected ships have an approximately %s chance to experience a malfunction every second. When a malfunction does occur the following effects may result: ", opad, h,
				"30%");
		text.addPara("%s chance to have a random engine become disabled. It is possible for a flameout to occur if enough engines are disabled.", opad, b,
				"45%");
		text.addPara("%s chance to have a random weapon become disabled. Will always affect a currently working weapon.", opad, b,
				"45%");
		text.addPara("%s chance to have a critical malfunction, causing an explosion that deals %s damage.", opad, b,
				"10%", "100 Energy");

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
