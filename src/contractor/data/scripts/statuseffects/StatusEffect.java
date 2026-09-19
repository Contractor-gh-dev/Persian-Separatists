package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

public interface StatusEffect extends AdvanceableListener {

	enum StatusType {
		CRYOGENIC,
		ELECTRIC,
		ENTROPIC,
		IMMUNE,
		JAMMED,
		NANITE_CORRUPTION,
		RADIATION,
		THERMAL,
		OTHER
	}

	/**
	 * @param effectLevel Value to modify effectLevel by, generally in seconds, can be negative.
	 */
	void addEffectLevel(float effectLevel);

	/**
	 * @param effectLevel Value to set effectLevel to. Generally in seconds.
	 */
	void setEffectLevel(float effectLevel);


	/**
	 * @return returns effectLevel remaining, generally in seconds.
	 */
	float getEffectLevel();

	/**
	 *  Removes status effect.
	 */
	void remove();

	/**
	 * @param withImmunity Add Immune status after removal.
	 */
	void remove(boolean withImmunity);

	/**
	 * @return Returns status effect type.
	 */
	StatusType getStatusType();
}
