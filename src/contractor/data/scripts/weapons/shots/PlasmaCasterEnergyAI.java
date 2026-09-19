package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

public class PlasmaCasterEnergyAI implements MissileAIPlugin, GuidedMissileAI {
	private final MissileAPI missile;

	public PlasmaCasterEnergyAI(MissileAPI missile, ShipAPI launchingShip) {
		this.missile = missile;
	}

	@Override
	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused() || missile.isFading() || missile.isFizzling()) {
			return;
		}

		missile.giveCommand(ShipCommand.ACCELERATE);
	}

	public CombatEntityAPI getTarget() {
		return null;
	}

	public void setTarget(CombatEntityAPI target) {
		return; //Ignore flares
	}

	@SuppressWarnings("EmptyMethod")
	public void init(CombatEngineAPI engine) {
	}
}
