def mapStructureBuilder = new local.MapStructureBuilder()

mapStructureBuilder
	.at('/')
		.getKey('a-b-c')
			.setKeyAs('list', [], 'A_list')

mapStructureBuilder
	.at('A_list')
		.addElement()
			.setKey('element', 'b-c-d')
			.setKey('element_1', 'value_1')
	.at('A_list')
		.addElement()
			.setKey('element', 'd-e-f')
			.setKey('element_2', 'value_2')
	.at('/')
		.getKey('f-g-h')
			.setKey('list', [])

println mapStructureBuilder
