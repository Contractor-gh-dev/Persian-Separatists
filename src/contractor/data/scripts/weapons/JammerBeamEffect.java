package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import contractor.data.scripts.statuseffects.Immune;
import contractor.data.scripts.statuseffects.Jammed;

import static contractor.data.scripts.util.ContractorStaticVars.STATUS_EFFECTS_ENABLED;

public class JammerBeamEffect implements BeamEffectPlugin {
	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		if (!STATUS_EFFECTS_ENABLED)
			return;
		CombatEntityAPI target = beam.getDamageTarget();
		ShipAPI source = beam.getSource();
		if (source == null)
			return;


		if (beam.getBrightness() >= 1f) {
			if (target instanceof ShipAPI shipTarget) {
				if (shipTarget.hasListenerOfClass(Immune.class))
					return;

				if (shipTarget.hasListenerOfClass(Jammed.class)) {
					boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getRayEndPrevFrame());
					Jammed plugin = shipTarget.getListeners(Jammed.class).get(0);
					if (hitShield) {
						if (plugin.getEffectLevel() < 0.5f)
							plugin.setEffectLevel(0.5f);
					} else
						plugin.setEffectLevel(1f);
				} else {
					shipTarget.addListener(new Jammed(shipTarget, 1f));
				}
			}
		}
	}
}
