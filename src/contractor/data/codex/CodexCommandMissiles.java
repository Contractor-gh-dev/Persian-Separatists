package contractor.data.codex;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.util.ContractorStaticVars;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class CodexCommandMissiles extends CodexEntryV2 implements CustomUIPanelPlugin {
	protected CustomPanelAPI panel;
	protected UIPanelAPI relatedEntries;
	protected UIPanelAPI box;
	protected CodexDialogAPI codex;

	public CodexCommandMissiles(String id, String title, String icon) {
		super(id, title, icon);
		addTag("All designs");
		addTag("Separatists");
	}

	@Override
	public void createTitleForList(TooltipMakerAPI info, float width, ListMode mode) {
		super.createTitleForList(info, width, mode);
	}

	@Override
	public boolean matchesTags(Set<String> tags) {
		for (String tag : tags)
			if (getTags().contains(tag))
				return true;

		return false;
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

		text.addPara("Command missiles require %s while in flight, if this number is exceeded for any reason, all command missiles lose guidance. " +
						"By default ships have %s/%s/%s/%s bandwidth, depending on hullsize. Ships equipped with an %s gain %s bandwidth, and ships with a %s gain %s bandwidth.", opad, h,
				"bandwidth",
				ContractorStaticVars.COMMAND_MISSILE_BASE.get(ShipAPI.HullSize.FRIGATE) + "",
				ContractorStaticVars.COMMAND_MISSILE_BASE.get(ShipAPI.HullSize.DESTROYER) + "",
				ContractorStaticVars.COMMAND_MISSILE_BASE.get(ShipAPI.HullSize.CRUISER) + "",
				ContractorStaticVars.COMMAND_MISSILE_BASE.get(ShipAPI.HullSize.CAPITAL_SHIP) + "",
				"ECCM", ContractorStaticVars.ECCM_BONUS + "",
				"Missile CIC", ContractorStaticVars.MC_BONUS + "");
		opad = 10f;

		text.addPara("Command missiles are controlled directly by the launching ships main targeting computer. As a result, they are very intelligent and can adjust their flight path dynamically while in flight. However, this comes at the " +
				"cost of requiring a dedicated connection and high processing cost, meaning very few can be in the air at the same time, particularly when hostile ECM is a factor.", opad);

		text.addPara("// CHIEF ENGINEERS ADDENDUM: Captain, if you're planning on letting the trigger happy boys on the gun deck manage these, recommend setting the controls to %s. Otherwise they'll just " +
						"overload the transmitters everytime the computers give 'em the green light.", opad, h,
				"ALTERNATING");

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

