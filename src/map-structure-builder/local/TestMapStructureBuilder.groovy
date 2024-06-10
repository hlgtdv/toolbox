import local.MapStructureBuilder

def mapStructureBuilder = new MapStructureBuilder()
//______________________________________________________________________________
//
mapStructureBuilder
	.at('/')
		.getEntry('a-b-c')
			.getListEntryAs('list', 'A_list')

mapStructureBuilder
	.at('A_list')
		.addElement()
			.setEntry('element', 'b-c-d')
			.setEntry('element_1', 'value_1')
	.at('A_list')
		.addElement()
			.setEntry('element', 'd-e-f')
			.setEntry('element_2', 'value_2')
	.at('/')
		.getEntry('f-g-h')
			.getListEntry('list')
	.at('A_list')
		.getElementAt(0)
	.at('A_list')
		.getElementAt(1)
			.setEntry('element_3', 'value_3')

// mapStructureBuilder.show()
//______________________________________________________________________________
//
def mapExistante = [ 'key3' : 3, 'key4' : 4 ]
def listExistante = [ 'value3', 'value4' ]
def v

mapStructureBuilder
	.reset()
	.addEntries('/.', 'key1', 1, 'key2', 2)
	.addEntries('/.', mapExistante)
	.debug()

	.reset()
	.addEntries('/*', 'value1', 'value2')
	.addEntries('/*', listExistante)
	.debug()

	.reset()
	.addEntries('/level1', 'key1', 1, 'key2', 2)
	.addEntries('/level1', mapExistante)
	.debug()

	.reset()
	.addEntries('/*/*', 'value1', 'value2')
	.addEntries('/*/*', listExistante)
	.debug()

	.reset()
	.addEntries('/list/*', 'value1', 'value2')
	.addEntries('/list/*', listExistante)
	.debug()

	.reset()
	.addEntries('/*/.', 'key1', 1, 'key2', 2)
	.addEntries('/*/.', mapExistante)
	.debug()

	.reset()
	.addEntries('/level1@lvl1', "key1", 1, "key2", 2)
	.addEntries("@lvl1/level2/.", "key3", 3)
	.debug()

println mapStructureBuilder.getValueAt("/level1")
println mapStructureBuilder.getValueAt("@lvl1/level2")
println mapStructureBuilder.getValueAt("/level1/key1")
println mapStructureBuilder.getValueAt("@lvl1/key1")
println mapStructureBuilder.getValueAt("@lvl1/level2/key3")

println mapStructureBuilder.getValueAt("/level1/key_UNKNOWN", 999)

try {
	// FIXME: No Exception thrown...
	println mapStructureBuilder.getValueAt("/*")
} catch (Exception e) { println "$e\n" }

mapStructureBuilder
	.reset()
	.addEntries('/*', 'value1', 'value2')
	.addEntries('/+/.')
	.debug()

println mapStructureBuilder.getValueAt("/*")
println mapStructureBuilder.getValueAt("/1")
println mapStructureBuilder.getValueAt("/2")
println mapStructureBuilder.getValueAt("/3")

try {
	println mapStructureBuilder.getValueAt("/9")
} catch (Exception e) { println "$e\n" }

mapStructureBuilder
	.reset()
	.addEntries("/*/*@lst1", "value1", "value2")
	.addEntries("@lst1", "value3")
	.debug()

	.traverse()
	.traverseFromPath("/*/*")
	.traverseFromContainer(mapStructureBuilder.getValueAt("/*/3"))

	.index()

	.reset()
	.addEntries("/.@lvl0", "key1", 1, "key2", 2)
	.addEntries("@lvl0", "key3", 3)
	.debug()

	.reset()
	.addEntries("/*@lst0", "value1", "value2")
	.addEntries("@lst0", "value3")
	.debug()

	.reset()
	.addEntries("/a@lvl1/b@lvl2_1/c@lvl3", "key1", 1, "key2", 2)
	.addEntries("@lvl1/d@lvl2_2", "key3", 3, "key4", 4)
	.debug()

mapStructureBuilder
	.traverse()
	.traverseFromPath("/a")
	.traverseFromContainer(mapStructureBuilder.getValueAt("/a/b"))

	.index()
	.indexFromPath("/a")
	.indexFromContainer(mapStructureBuilder.getValueAt("/a/b"))

mapStructureBuilder
	.reset()
	.addEntries("/+/.",
		'@class', 'AItem',
		'id', 1,
		'label', 'AItem 1 label')
	.addEntries("/+/.",
		'@class', "BItem",
		'id', 1,
		'label', 'BItem 1 label')
	.addEntries("/+/.",
		'@class', 'AItem',
		'id', 2,
		'label', 'AItem 2 label')
	.addEntries("/+/.",
		'@class', 'BItem',
		'id', 2,
		'label', 'BItem 2 label')
	.addEntries("/+/.",
		'@class', 'CItem',
		'id', 1,
		'label', 'CItem 1 label')

	.index()

def CONDITION = { element -> element?.'@class' != 'BItem' }

mapStructureBuilder
	.traverseWhere(CONDITION)
	.traverseFromPathWhere("/*", CONDITION)
	.traverseFromContainerWhere(mapStructureBuilder.getValueAt("/*"), CONDITION)

	.indexWhere(CONDITION, false)
	.indexFromPathWhere("/*", CONDITION, false)
	.indexFromContainerWhere(mapStructureBuilder.getValueAt("/*"), CONDITION, false)
