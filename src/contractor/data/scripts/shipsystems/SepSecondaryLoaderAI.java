package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

public class SepSecondaryLoaderAI implements ShipSystemAIScript {
	private int numLarge = 0;
	private float timer = 0f;
	private ShipAPI ship;
	private final List<WeaponAPI> validWeps = new ArrayList<>();
	private static final HashMap<WeaponAPI.WeaponSize, Float> scale = new HashMap<>(); static {
		scale.put(WeaponAPI.WeaponSize.SMALL, 0.1f);
		scale.put(WeaponAPI.WeaponSize.MEDIUM, 0.225f);
		scale.put(WeaponAPI.WeaponSize.LARGE, 0.45f);
	}

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;

		for (WeaponAPI wep : ship.getAllWeapons()) {
			if (wep.isDisabled())
				continue;

			EnumSet<WeaponAPI.AIHints> hints = wep.getOriginalSpec().getAIHints();
			if (hints.contains(WeaponAPI.AIHints.PD) && !hints.contains(WeaponAPI.AIHints.PD_ALSO))
				continue;

			if ((wep.getType() == WeaponAPI.WeaponType.BALLISTIC || wep.getType() == WeaponAPI.WeaponType.HYBRID || wep.getType() == WeaponAPI.WeaponType.COMPOSITE) && !wep.getSpec().isBeam()) {
				validWeps.add(wep);

				if (wep.getSize() == WeaponAPI.WeaponSize.LARGE)
					numLarge++;
			}
		}
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		timer += amount;
		if (timer > 0.2f) {
			timer = 0f;

			if (AIUtils.canUseSystemThisFrame(ship)) {
				float desire = 0f, ammoMod = 1f;

				for (WeaponAPI wep : validWeps) {
					if (wep.usesAmmo() && wep.getAmmo() == 0 && wep.getSpec().getReloadSize() > 0)
						ammoMod += 0.1f;
					if (wep.getCooldownRemaining() > SepSecondaryLoader.COOLDOWN_LIMIT) {
						if (wep.getSize() == WeaponAPI.WeaponSize.LARGE)
							desire += (wep.getCooldownRemaining() * scale.get(wep.getSize())) / numLarge * 0.75f;
						else
							desire += wep.getCooldownRemaining() * scale.get(wep.getSize());
					}
				}

				desire *= ammoMod;

				if (desire > 1)
					ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			}
		}
	}
}
