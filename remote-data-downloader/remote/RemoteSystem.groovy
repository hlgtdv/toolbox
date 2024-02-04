package remote

class RemoteSystem {

	def items = [
		'''
		<definition type='CONTAINER_1' id='Container1' version='1.0'>
			<modified-timestamp>1707048140</modified-timestamp>
			<content>
				<item-ref type='CONTAINER_2' id='Container2' />
			</content>			
		</definition>
		''',
		'''
		<definition type='CONTAINER_2' id='Container2' version='1.0'>
			<modified-timestamp>1707048141</modified-timestamp>
			<content>
				<item-ref type='CONTAINER_3' id='Container3' />
			</content>			
		</definition>
		''',
		'''
		<definition type='CONTAINER_3' id='Container3' version='1.0'>
			<modified-timestamp>1707048142</modified-timestamp>
			<content>
				<item-ref type='ELEMENT_1' id='Element1' />
			</content>			
		</definition>
		''',
		'''
		<definition type='ELEMENT_1' id='Element1' version='1.0'>
			<modified-timestamp>1707048143</modified-timestamp>
			<content>
				<item-ref type='ELEMENT_2' id='Element2' />
			</content>			
		</definition>
		''',
		'''
		<definition type='ELEMENT_2' id='Element2' version='1.0'>
			<modified-timestamp>1707048144</modified-timestamp>
			<content>
				<item-ref type='ELEMENT_3' id='Element3' />
			</content>			
		</definition>
		''',
		'''
		<definition type='ELEMENT_3' id='Element3' version='1.0'>
			<modified-timestamp>1707048145</modified-timestamp>
		</definition>
		''',
	]
	
	def findItem(type, id) {
		
		for (item in items) {
			if (item.contains("definition type='${type}' id='${id}'")) {
					return item
			}
		}
	}
}
