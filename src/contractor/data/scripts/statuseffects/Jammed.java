package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Jammed extends BaseStatusEffect {
	private final List<WeaponAPI> eWeapons = new ArrayList<>();

	public Jammed(@NotNull ShipAPI target, float initial) {
		super(target, initial);
		this.id = target.getId() + StatusType.JAMMED;
		this.type = StatusType.JAMMED;
		for (WeaponAPI weps : target.getAllWeapons()) {
			if (!weps.getSlot().isHidden())
				eWeapons.add(weps);
		}
	}

	public void advance(float amount) {
		if (!target.isAlive()) {
			remove();
			return;
		}

		float jamDebuff = 20f;
		target.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, -jamDebuff);
		target.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(id, -jamDebuff);
		target.getMutableStats().getBeamWeaponRangeBonus().modifyPercent(id, -jamDebuff);
		target.getMutableStats().getSightRadiusMod().modifyPercent(id, -jamDebuff);

		CombatEngineAPI engine = Global.getCombatEngine();
		for (WeaponAPI weps : eWeapons) {
			if (Math.random() > 0.5f) {
				float angle = (float) (Math.random() * 360f);
				Vector2f vel = Misc.getUnitVectorAtDegreeAngle(angle);
				vel.scale((float) (Math.random() * 128 + 32f));
				engine.addHitParticle(weps.getLocation(), vel, 8f, 0.8f, 0.5f, Color.orange);
			}
		}

		boolean isPlayer = target == Global.getCombatEngine().getPlayerShip();

		if (isPlayer)
			Global.getCombatEngine().maintainStatusForPlayerShip(id, Global.getSettings().getSpriteName("systems", "drg_jam_ui_sprite"), "EWAR Jamming", Math.round(jamDebuff) + "% RANGE DOWN", true);

		float timeMult;
		if (isPlayer)
			timeMult = 1f;
		else
			timeMult = target.getMutableStats().getTimeMult().modified;

		effectLevel -= amount * timeMult;
		if (effectLevel <= 0f)
			remove();
	}


	@Override
	public void remove() {
		target.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
		target.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
		target.getMutableStats().getBeamWeaponRangeBonus().unmodify(id);
		target.getMutableStats().getSightRadiusMod().unmodify(id);
		target.removeListener(this);
	}
}
