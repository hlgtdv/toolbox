def mapStructureBuilder = new local.MapStructureBuilder()

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

println mapStructureBuilder
