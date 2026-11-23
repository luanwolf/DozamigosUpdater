// Zenith: only swords whose whole recipe chain can be crafted.
craftingTable.removeByName("cenitforge:zenith_recipe");

craftingTable.addShaped("dozamigos_zenith", <item:cenitforge:zenith>, [
    [<item:epicsamurai:katana_netherite>, <item:twilightforest:ironwood_sword>, <item:stalwart_dungeons:chorundum_sword>],
    [<item:create_sa:rose_quartz_sword>, <item:minecraft:netherite_sword>, <item:create_sa:experience_sword>],
    [<item:eidolon:silver_sword>, <item:stalwart_dungeons:tungsten_sword>, <item:create_sa:brass_sword>]
]);
