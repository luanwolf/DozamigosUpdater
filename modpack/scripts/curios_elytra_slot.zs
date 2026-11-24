#priority 50

// Dedicated curios:elytra slot — keep elytras out of curios:back.
// Elytra Slot still grants flight from any curios slot.

<tag:items:curios:elytra>.add(<item:minecraft:elytra>);
<tag:items:curios:elytra>.add(<item:alexsmobs:tarantula_hawk_elytra>);
<tag:items:curios:elytra>.add(<item:deeperdarker:soul_elytra>);
<tag:items:curios:elytra>.add(<item:enigmaticlegacy:enigmatic_elytra>);

<tag:items:curios:back>.remove(<item:minecraft:elytra>);
<tag:items:curios:back>.remove(<item:alexsmobs:tarantula_hawk_elytra>);
<tag:items:curios:back>.remove(<item:deeperdarker:soul_elytra>);
<tag:items:curios:back>.remove(<item:enigmaticlegacy:enigmatic_elytra>);
