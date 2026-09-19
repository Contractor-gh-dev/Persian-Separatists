package contractor.data.codex;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;

import static contractor.data.scripts.util.ContractorStaticVars.COMMAND_KEY;

public class CodexCommandMissilesModes extends CodexEntryV2 implements CustomUIPanelPlugin {
	protected CustomPanelAPI panel;
	protected UIPanelAPI relatedEntries;
	protected UIPanelAPI box;
	protected CodexDialogAPI codex;

	public CodexCommandMissilesModes(String id, String title, String icon) {
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

		String commandKeyString = Keyboard.getKeyName(COMMAND_KEY).toUpperCase();

		text.addPara("Player ships equipped with a Missile CIC can switch command modes by pressing %s. Brief explanation of command missile flight modes: ", opad, h,
				"[" + commandKeyString + "]");
		opad = 8f;

		text.addPara("%s: Missile will try to select one of the following modes based on the current situation. Non-player ships use this mode.", opad, h,
				"Smart");

		text.addPara("%s: Missile will circle around the target outside point defenses and attempt to strike in an unshielded area, if it can't find a spot, it will instead aim for the rear.", opad, h,
				"Flank");

		text.addPara("%s: Missile will follow a curved path to either of the targets sides, to strike quickly while still avoiding point defenses.", opad, h,
				"Side Attack");

		text.addPara("%s: Missile will circle around the target outside point defenses and attempt to strike the tail, or will strike when low on fuel.", opad, h,
				"Tail Strike");

		text.addPara("%s: Missile will fly directly at the target while weaving slightly.", opad, h,
				"Direct");

		text.addPara("%s: Missile will circle around the target outside point defenses and wait for the target to overload, or will strike when low on fuel.", opad, h,
				"Vulture");

		text.addPara("%s: Only available on player captained ships. Missile will follow the players aim point.", opad, h,
				"Laser Guided");


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

