package contractor.data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;

public class NaniteRespiteScript implements EconomyUpdateListener {

	public static final String KEY = "$con_nanite_resp_ref";

	public static float DURATION = 540;
	//public static float DURATION = -1f;

	public static NaniteRespiteScript get() {
		//if (true) return null;
		return (NaniteRespiteScript) Global.getSector().getMemoryWithoutUpdate().get(KEY);
	}

	public static boolean playerHasPiracyRespite() {
		return get() != null;
	}


	protected long timestamp;

	public NaniteRespiteScript() {
		sendGainedMessage();

		// to avoid duplicates
		NaniteRespiteScript existing = get();
		if (existing != null) {
			existing.resetTimestamp();
			return;
		}

		resetTimestamp();
		Global.getSector().getEconomy().addUpdateListener(this);
		Global.getSector().getMemoryWithoutUpdate().set(KEY, this);

		economyUpdated();
	}

	public void sendGainedMessage() {
		MessageIntel msg = new MessageIntel();
		msg.addLine("Nanite Respite gained", Misc.getBasePlayerColor());

		msg.addLine(BaseIntelPlugin.BULLET + "Reduced colony threat", Misc.getTextColor());

		msg.setIcon(Global.getSettings().getSpriteName("events", "piracy_respite"));
		msg.setSound("ui_intel_update");
		Global.getSector().getCampaignUI().addMessage(msg, MessageClickAction.COLONY_INFO);
	}

	public void sendExpiredMessage() {
		MessageIntel msg = new MessageIntel();
		msg.addLine("Nanite Respite expired", Misc.getBasePlayerColor());
		msg.setIcon(Global.getSettings().getSpriteName("events", "piracy_respite"));
		msg.setSound("ui_intel_update");
		Global.getSector().getCampaignUI().addMessage(msg, MessageClickAction.COLONY_INFO);
	}

	public void resetTimestamp() {
		timestamp = Global.getSector().getClock().getTimestamp();
	}

	public float getDaysRemaining() {
		if (DURATION < 0) return DURATION;
		float rem = DURATION - Global.getSector().getClock().getElapsedDaysSince(timestamp);
		//rem = 1f - Global.getSector().getClock().getElapsedDaysSince(timestamp);
		if (rem < 0) rem = 0;
		return rem;
	}

	public void commodityUpdated(String commodityId) {
	}

	public void economyUpdated() {
		//for (MarketAPI curr : Misc.getPlayerMarkets(false)) {
		for (MarketAPI curr : Global.getSector().getEconomy().getMarketsCopy()) {
			if (curr.isPlayerOwned() && curr.getFaction() != null && curr.getFaction().isPlayerFaction()) {
				if (!curr.hasCondition(Conditions.PIRACY_RESPITE)) {
					curr.addCondition(Conditions.PIRACY_RESPITE);
				}
			} else {
				if (curr.hasCondition(Conditions.PIRACY_RESPITE)) {
					curr.removeCondition(Conditions.PIRACY_RESPITE);
				}
			}
		}
	}

	public void cleanup() {
		if (Global.getSector().getMemoryWithoutUpdate().contains(KEY)) {
			sendExpiredMessage();
		}
		Global.getSector().getMemoryWithoutUpdate().unset(KEY);
	}

	public boolean isEconomyListenerExpired() {
		if (DURATION < 0) return false;

		float days = getDaysRemaining();
		if (days <= 0) {
			cleanup();
			return true;
		}
		return false;
	}
}



