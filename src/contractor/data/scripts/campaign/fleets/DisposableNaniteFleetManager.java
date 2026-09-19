package contractor.data.scripts.campaign.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.CurrentLocationChangedListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.fleets.DisposableFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import contractor.data.scripts.util.ContractorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

import static contractor.data.scripts.util.ContractorStaticVars.*;

public class DisposableNaniteFleetManager extends DisposableFleetManager implements CurrentLocationChangedListener {
	public static float DIST_0 = 5f;
	public static float DIST_1 = 10f;
	public static float DIST_2 = 15f;

	public static class NaniteFleetCreationParams {
		public int numUnification = 0;
		public int numCapitals = 0;
		public int numCruisers = 0;
		public int numDestroyers = 0;

		public String fleetType = FleetTypes.PATROL_SMALL;
	}

	/**
	 * In $player memory.
	 */
	public static String SENSOR_MODS_KEY = "$hasThreatDetectionSensorMods";

	public static int MIN_FLEETS = 1;
	public static int MAX_FLEETS = 5;


	public DisposableNaniteFleetManager() {
		Global.getSector().getListenerManager().addListener(this);
	}

	protected Object readResolve() {
		super.readResolve();
		return this;
	}

	@Override
	protected String getSpawnId() {
		return CONTRACTOR_NANITE_ID;
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		// want Nanite fleets to basically "be there" not gradually spawn in
		if (spawnRateMult > 0) {
			spawnRateMult = 1000f;
		}
	}

	@Override
	public void reportCurrentLocationChanged(LocationAPI prev, LocationAPI curr) {
		if (tracker2 != null) {
			tracker2.forceIntervalElapsed();
		}
	}

	@Override
	protected float getExpireDaysPerFleet() {
		return 365f; // don't spawn again for a long time after reaching max
	}

	@Override
	protected int getDesiredNumFleetsForSpawnLocation() {
		String id = currSpawnLoc.getOptionalUniqueId();
		if (id == null) id = currSpawnLoc.getId();

		Random random = new Random(id.hashCode() * 1343890532L);
		StarSystemAPI homeSys = (StarSystemAPI) Global.getSector().getPersistentData().get(CONTRACTOR_NANITE_HOME_SYSTEM);
		if (homeSys == null) {
			Global.getLogger(DisposableNaniteFleetManager.class).warn("Cannot locate NANITE home system, preventing spawn.");
			return 0;
		}
		float dist = Misc.getDistanceLY(currSpawnLoc.getLocation(), homeSys.getLocation());
		if (dist > DIST_2 + 5f) return 0;

		if (currSpawnLoc.hasTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY)
				|| currSpawnLoc.hasTag(Tags.THEME_CORE)
				|| currSpawnLoc.hasTag(Tags.THEME_REMNANT)
				|| currSpawnLoc.hasTag(Tags.THREAT)
				|| currSpawnLoc.hasTag(CONTRACTOR_NANITE_NO_SPAWN))
			return 0;

		float maxDepth = 20f;

		float f = maxDepth / (maxDepth + dist);
		if (f > 1f) f = 1f;

		int minFleets = 1;
		int maxFleets = MIN_FLEETS + Math.round((MAX_FLEETS - MIN_FLEETS) * f);

		return minFleets + random.nextInt(maxFleets - minFleets + 1);
	}

	@Override
	protected boolean withReturnToSourceAssignments() {
		return false;
	}

	protected StarSystemAPI pickCurrentSpawnLocation() {
		if (Global.getSector().isInNewGameAdvance()) return null;
		CampaignFleetAPI player = Global.getSector().getPlayerFleet();
		if (player == null) return null;
		StarSystemAPI nearest = null;
		float minDist = Float.MAX_VALUE;

		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			//if (!system.hasTag(Tags.SYSTEM_CAN_SPAWN_THREAT)) continue;
			float distToPlayerLY = Misc.getDistanceLY(player.getLocationInHyperspace(), system.getLocation());
			if (distToPlayerLY > MAX_RANGE_FROM_PLAYER_LY) continue;

			if (distToPlayerLY < minDist) {
				nearest = system;
				minDist = distToPlayerLY;
			}
		}

		// stick with current system longer unless something else is closer
		if (nearest == null && currSpawnLoc != null) {
			float distToPlayerLY = Misc.getDistanceLY(player.getLocationInHyperspace(), currSpawnLoc.getLocation());
			if (distToPlayerLY <= DESPAWN_RANGE_LY)
				nearest = currSpawnLoc;
		}
		return nearest;
	}


	protected CampaignFleetAPI spawnFleetImpl() {
		if (Global.getSettings().isDevMode())
			Global.getLogger(DisposableNaniteFleetManager.class).info("Attempting to spawn NANITE fleet...");
		StarSystemAPI system = currSpawnLoc;
		//StarSystemAPI homeSys = (StarSystemAPI) Global.getSector().getPersistentData().get(CONTRACTOR_NANITE_HOME_SYSTEM);
		if (system == null) return null;

		CampaignFleetAPI player = Global.getSector().getPlayerFleet();
		if (player == null) return null;

		int numSecond = 0;
		int maxSecond = 1;
		int numThird = 0;
		for (CampaignFleetAPI fleet : currSpawnLoc.getFleets()) {
			if (fleet.getFaction().getId().equals(CONTRACTOR_NANITE_ID)) {
				String type = Misc.getFleetType(fleet);
				if (type == null) continue;
				if (type.equals(FleetTypes.PATROL_MEDIUM))
					numSecond++;
				else if (type.equals(FleetTypes.PATROL_LARGE))
					numThird++;
			}
		}

		int distFactor = ContractorUtils.computeNaniteDistFactor(system);
		//float dist = Misc.getDistanceLY(system.getLocation(), homeSys.getLocation());
		if (distFactor == 0 && (float) Math.random() < 0.5f) maxSecond = 2;

		if (numThird > 0) {
			distFactor = Math.max(distFactor, 1);
		}
		if (numSecond > maxSecond) {
			if ((float) Math.random() < 0.5f)
				distFactor = Math.max(distFactor, 1);
			else
				distFactor = Math.max(distFactor, 2);
		}

//		if (numThird > 0) {
//			dist = Math.max(dist, DIST_0 + 0.1f);
//		}
//		if (numSecond > maxSecond) {
//			if ((float) Math.random() < 0.5f)
//				dist = Math.max(dist, DIST_0 + 0.1f);
//			else
//				dist = Math.max(dist, DIST_1 + 0.1f);
//		}

		WeightedRandomPicker<DisposableThreatFleetManager.FabricatorEscortStrength> picker = new WeightedRandomPicker<>();
		DisposableThreatFleetManager.FabricatorEscortStrength strength;
		int unifications;

		switch (distFactor) {
			case 0 -> {
				unifications = 2;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 5f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM, 3f);
				strength = picker.pick();
				if (Math.random() > 0.6d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 3;
				}
			}
			case 1 -> {
				unifications = 1;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 1f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 5f);
				strength = picker.pick();
				if (Math.random() > 0.7d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 2;
				}
			}
			case 2 -> {
				unifications = 0;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.NONE, 5f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 15f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 3f);
				strength = picker.pick();
				if (Math.random() > 0.8d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 1;
				}
			}
			default -> {
				unifications = 0;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.NONE, 15f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 1f);
				strength = picker.pick();
				if (Math.random() > 0.9d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 1;
				}
			}
		}

//		if (distFactor == 0) {
//			unifications = 2;
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 5f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM, 3f);
//			strength = picker.pick();
//			if (Math.random() > 0.6d &&
//					(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
//				unifications = 3;
//			}
//		} else if (distFactor == 1) {
//			unifications = 1;
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 1f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 10f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 5f);
//			strength = picker.pick();
//			if (Math.random() > 0.7d &&
//					(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
//				unifications = 2;
//			}
//		} else if (distFactor == 2) {
//			unifications = 0;
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.NONE, 5f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 15f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 10f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 3f);
//			strength = picker.pick();
//			if (Math.random() > 0.8d &&
//					(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
//				unifications = 1;
//			}
//		} else {
//			unifications = 0;
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.NONE, 15f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
//			picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 1f);
//			strength = picker.pick();
//			if (Math.random() > 0.9d &&
//					(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
//				unifications = 1;
//			}
//		}

		CampaignFleetAPI f = createNaniteFleet(unifications, strength);
		if (f == null)
			return null;

		system.addEntity(f);

		float radius = 4000f + 2000f * (float) Math.random();
		Vector2f loc = Misc.getPointAtRadius(new Vector2f(), radius);
		f.setLocation(loc.x, loc.y);

		f.addScript(new NaniteFleetBehaviorScript(f, system));
		if (Global.getSettings().isDevMode())
			Global.getLogger(DisposableNaniteFleetManager.class).info("NANITE fleet created!");

		return f;
	}

	public static CampaignFleetAPI createNaniteFleet(int numUnification, DisposableThreatFleetManager.FabricatorEscortStrength escorts) {
		if (escorts == DisposableThreatFleetManager.FabricatorEscortStrength.NONE)
			return null;

		Random random = Misc.random;

		int minCapitals = 0;
		int maxCapitals = 0;
		int minCruisers = 0;
		int maxCruisers = 0;
		int minDestroyers = 0;
		int maxDestroyers = 0;

		switch (escorts) {
			case LOW -> {
				maxCruisers = 1;
				minDestroyers = 3;
				maxDestroyers = 5;
				if (numUnification <= 0) {
					minCruisers = 1;
				}
			}
			case MEDIUM -> {
				maxCapitals = 1;
				minCruisers = 1;
				maxCruisers = 3;
				minDestroyers = 3;
				maxDestroyers = 6;
				if (numUnification <= 0) {
					minCapitals = 1;
				}
			}
			case HIGH -> {
				minCapitals = 1;
				maxCapitals = 2;
				minCruisers = 2;
				maxCruisers = 4;
				minDestroyers = 3;
				maxDestroyers = 6;
			}
			case MAXIMUM -> {
				minCapitals = 2;
				maxCapitals = 3;
				minCruisers = 3;
				maxCruisers = 5;
				minDestroyers = 4;
				maxDestroyers = 7;
				if (numUnification >= 3) {
					maxCapitals = 2;
				}
			}
		}

		NaniteFleetCreationParams params = new NaniteFleetCreationParams();
		params.numUnification = numUnification;
		params.numCapitals = minCapitals + random.nextInt(maxCapitals - minCapitals + 1);
		params.numCruisers = minCruisers + random.nextInt(maxCruisers - minCruisers + 1);
		params.numDestroyers = minDestroyers + random.nextInt(maxDestroyers - minDestroyers + 1);

		params.fleetType = FleetTypes.PATROL_SMALL;
		if (numUnification >= 3 ||
				(numUnification == 2 && escorts.ordinal() >= DisposableThreatFleetManager.FabricatorEscortStrength.HIGH.ordinal())) {
			params.fleetType = FleetTypes.PATROL_LARGE;
		} else if (numUnification == 2 ||
				(numUnification == 1 && escorts.ordinal() >= DisposableThreatFleetManager.FabricatorEscortStrength.HIGH.ordinal())) {
			params.fleetType = FleetTypes.PATROL_MEDIUM;
		}

		return createNaniteFleet(params, random);
	}

	protected static CampaignFleetAPI createNaniteFleet(NaniteFleetCreationParams params, Random random) {
		CampaignFleetAPI f = Global.getFactory().createEmptyFleet(CONTRACTOR_NANITE_ID, "Host", true);
		f.setInflater(null);
		f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_TYPE, params.fleetType);

		addUnis(f, params.numUnification, "naniteBigCapital", random);
		addShips(f, params.numCapitals, ShipRoles.COMBAT_CAPITAL, random);
		addShips(f, params.numCruisers, ShipRoles.COMBAT_LARGE, random);
		addShips(f, params.numDestroyers, ShipRoles.COMBAT_MEDIUM, random);
		f.getFleetData().setSyncNeeded();
		f.getFleetData().syncIfNeeded();
		f.getFleetData().sort();

		for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
			curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
			curr.setPersonalityOverride(Personalities.RECKLESS);
		}

		FactionAPI faction = Global.getSector().getFaction(CONTRACTOR_NANITE_ID);
		f.setName(faction.getFleetTypeName(params.fleetType));

		f.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, new NaniteFIDConfig());
		f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
		f.getMemoryWithoutUpdate().set(MemFlags.MAY_GO_INTO_ABYSS, true);

		return f;
	}

	public static void addUnis(CampaignFleetAPI fleet, int num, String role, Random random) {
		FactionAPI faction = Global.getSector().getFaction(CONTRACTOR_NANITE_ID);
		ShipPickParams p = new ShipPickParams(ShipPickMode.ALL);
		p.blockFallback = true;
		p.maxFP = 1000000;

		for (int i = 0; i < num; i++) {
			List<ShipRolePick> picks = faction.pickShip(role, p, null, random);
			for (ShipRolePick pick : picks) {
				fleet.getFleetData().addFleetMember(pick.variantId);
			}
		}
	}

	public static void addShips(CampaignFleetAPI fleet, int num, String role, Random random) {
		FactionAPI faction = Global.getSector().getFaction(CONTRACTOR_NANITE_ID);
		ShipPickParams p = new ShipPickParams(ShipPickMode.PRIORITY_ONLY);
		p.blockFallback = true;
		p.maxFP = 1000000;

		for (int i = 0; i < num; i++) {
			List<ShipRolePick> picks = faction.pickShip(role, p, null, random);
			for (ShipRolePick pick : picks) {
				fleet.getFleetData().addFleetMember(pick.variantId);
			}
		}
	}
}








