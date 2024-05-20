package local

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions

public class StructureBuilder {

	static def DEFAULT_TRAVERSAL_HANDLER = [
		beforeTraversal : {
			println "[Before     ] Start of traversal ${'-' * 100}"
		},
		beforeMapTraversal : { level, path, element ->
			println "[Before Map ] At level ${level}, ${path} -> ${element} : ${element.getClass().simpleName}"
		},
		afterMapTraversal : { level, path, element ->
			println "[After Map  ] At level ${level}, ${path} -> ${element} : ${element.getClass().simpleName}"
		},
		beforeListTraversal : { level, path, element ->
			println "[Before List] At level ${level}, ${path} -> ${element} : ${element.getClass().simpleName}"
		},
		afterListTraversal : { level, path, element ->
			println "[After List ] At level ${level}, ${path} -> ${element} : ${element.getClass().simpleName}"
		},
		onElementTraversal : { level, path, element ->
			println "[Element    ] At level ${level}, ${path} -> ${element} : ${element.getClass().simpleName}"
		},
		afterTraversal : {
			println "[After      ] End of traversal ${'-' * 102}"
		}		
	]
	static def INDEX_TRAVERSAL_HANDLER = [
		beforeMapTraversal : { level, path, element ->
			println "${path} -> ${element.getClass().simpleName}: { *=${element.size()}"
		},
		beforeListTraversal : { level, path, element ->
			println "${path} -> ${element.getClass().simpleName}: [ *=${element.size()}"
		},
		onElementTraversal : { level, path, element ->
			def value

			if (element != null) {
				if (element instanceof String) {
					def p = element.indexOf("\n")
					value = p > -1 ? element.substring(0, p) + " [...]" : element
				}
				else {
					value = element
				}
			}
			println "${path} -> ${element.getClass().simpleName}: ${value}"
		},
	]
	static def RE_IDENTIFIER = /@?\w+([-:]\w+)*/

	def yaml
	def structure
	def aliasIndex
	def path
	def parsedPath
	def elements

	StructureBuilder() {
		def dumperOptions = new DumperOptions()
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)

		this.yaml = new Yaml(dumperOptions)
		this.reset()
	}

	private def lookAhead(token) {
		return this.parsedPath.startsWith(token)
	}

	private def lookAheadAsRegex(token) {
		def regex = "^${token}"
		def m = this.parsedPath =~ regex

		return m.asBoolean()
	}

	private def readToken(token) {
		def tokenSize = token.size()
		def readToken = this.parsedPath.substring(0, tokenSize)

		if (readToken != token) {
			throw new RuntimeException("Unexpected token [${readToken}] found instead of [${token}] in [${this.path}<EOL>]")
		}
		this.parsedPath = this.parsedPath.substring(tokenSize)

		return readToken
	}

	private def readTokenAsRegex(token) {
		def regex = "^${token}"
		def m = this.parsedPath =~ regex

		if (! m.asBoolean()) {
			throw new RuntimeException("Token [${regex}] not matched in [${this.path}]<EOL>")
		}
		def readToken = m.group(0)
		this.parsedPath = this.parsedPath.substring(readToken.size())

		return readToken
	}

	private def parsePath(path) {
		this.parsedPath = "${path}<EOL>"

		def identifier
		def integer
		def token

		if (this.lookAhead('@')) {
			this.readToken('@')
			identifier = this.readTokenAsRegex(RE_IDENTIFIER)
                                    							this.manageAlias(identifier, true)
		}

		while (! this.lookAhead('<EOL>')) {
			this.readToken('/')

			if (this.lookAhead('*')) {
				token = this.readToken('*')
																this.manageElementBy(token)
			}
			else if (this.lookAhead('+')) {
				token = this.readToken('+')
																this.manageElementBy(token)
			}
			else if (this.lookAheadAsRegex(/\d+/)) {
				integer = this.readTokenAsRegex(/\d+/)
																this.manageElementBy(integer)
			}
			else if (this.lookAheadAsRegex(RE_IDENTIFIER)) {
				identifier = this.readTokenAsRegex(RE_IDENTIFIER)
																this.manageElementBy(identifier)
			}
			else {
				token = this.readToken('.')
																this.manageElementBy(token)
			}

			if (this.lookAhead('@')) {
				this.readToken('@')
				identifier = this.readTokenAsRegex(RE_IDENTIFIER)
																this.manageAlias(identifier) 
			}
		}
		this.readToken('<EOL>')
	}

	private def manageElementBy(accessor) {
		def text = accessor
		def type

		if (accessor ==~ /\d+/) {
			type = List
			accessor = accessor as Integer
		}
		else if (accessor ==~ RE_IDENTIFIER) {
			type = Map
		}
		else if (accessor == '.') {
			type = Map
			accessor = null
		}
		else if (accessor == '*') {
			type = List
			accessor = -1	// Last element
		}
		else if (accessor == '+') {
			type = List
			accessor = -9	// New element
		}
		this.elements << [
				'text': "/${text}",
				'type': type,
				'accessor': accessor,
				'alias' : null
			]
	}

	private def manageAlias(alias, create = false) {
		if (create) {
			this.elements << [
				'text': "@${alias}",
				'type': "Alias",
				'name' : alias
			]
		}
		else {
			this.elements[this.elements.size() - 1].alias = alias
		}
	}

	private def normalizeElements(createIfAbsent) {
		def element = this.elements[this.elements.size() - 1]

		if (element.type == List && element.accessor == -1) {
			element.accessor = null
		}
		else if (element.type == Map && element.accessor != null) {
			this.elements << [
				'text': null,
				'type': createIfAbsent ? element.type : null,
				'accessor': null,
				'alias' : null
			]
		}
	}

	private def fetchCurrentElement(createIfAbsent) {
		this.fetchCurrentElementFromContainer(this.structure, createIfAbsent)
	}

	private def fetchCurrentElementFromContainer(container, createIfAbsent) {
		def currentElement = container
		def elem = this.elements[0]
		def checkType = true

		if (elem.type == "Alias") {
			currentElement = this.aliasIndex[elem.name]
			checkType = false
		}
		else if (currentElement == null) {
			currentElement = this.structure = elem.type == Map ? [:] : []
		}
		this.elements.eachWithIndex { element, i ->
			if (element.type != null && element.type != "Alias" && ! element.type.isInstance(currentElement)) {
				throw new RuntimeException("Type [${currentElement.getClass().simpleName}] found whereas type"
					+ " [${element.type.simpleName}] expected at path [${this.path}]")
			}

			if (element.accessor != null) {
				def nextElement = this.elements[i + 1]
				def accessor
				def e

				if (element.accessor instanceof Integer) {
					if (! (currentElement instanceof List)) {
						throw new RuntimeException("List accessor not allowed with the type"
							+ " [${currentElement.getClass().simpleName}] in path [${this.path}]")
					}

					if (element.accessor == -1) {
						accessor = Math.max(0, currentElement.size() - 1)
					}
					else if (element.accessor == -9) {
						currentElement.add(null)
						accessor = currentElement.size() - 1
					}
					else {
						accessor = element.accessor - 1

						if (! createIfAbsent) {
							if (currentElement.size() == 0) {
								throw new RuntimeException("List accessor used with empty list in path [${this.path}]")
							}
							else if (accessor >= currentElement.size()) {
								throw new RuntimeException("List accessor not in [1 .. ${currentElement.size()}]"
									+ " in path [${this.path}]")
							}
						}
					}
				}
				else {
					accessor = element.accessor

					if (! (currentElement instanceof Map)) {
						throw new RuntimeException("Map accessor not allowed with the type"
							+ " [${currentElement.getClass().simpleName}] in path [${this.path}]")
					}

					if (! createIfAbsent && ! (accessor in currentElement)) {
						throw new RuntimeException("Map accessor not found in ${currentElement.keySet()}"
							+ " in path [${this.path}]")
					}
				}
				e = currentElement[accessor]

                if (e == null) {
	                if (createIfAbsent) {
						if (nextElement == null) {
							throw new RuntimeException("Type of the element to be created missing in path [${this.path}]")
						}
	                    e = currentElement[accessor] = nextElement.type == Map ? [:] : []
	                }
                }
                else {
					if (nextElement != null && nextElement.type != null && ! nextElement.type.isInstance(e)) {
						throw new RuntimeException("Type [${e.getClass().simpleName}] found whereas type"
							+ " [${nextElement.type}] expected at path [${this.path.simpleName}]")
					}
                }
                currentElement = e
			}

			if (element.alias != null) {
				this.aliasIndex[element.alias] = currentElement
			}
		}
		return currentElement
	}

	private def getElementAt(path, createIfAbsent = true) {
		this.path = path

		this.reset(false)
		this.parsePath(path)
		this.normalizeElements(createIfAbsent)

		return this.fetchCurrentElement(createIfAbsent)
	}

	private def getElementFromContainerAt(container, path, createIfAbsent = true) {
		this.path = path

		this.reset(false)
		this.parsePath(path)
		this.normalizeElements(createIfAbsent)

		return this.fetchCurrentElementFromContainer(container, createIfAbsent)
	}

	private def addValuesAsMapContainerEntries(Object[] values, container) {
		def key

		values.eachWithIndex { keyOrValue, i ->
			if (i % 2 == 0) {
				key = keyOrValue
			}
			else {
				container[key] = keyOrValue
			}
		}
	}

	private def addValuesAsListContainerEntries(Object[] values, container) {
		values.each { value ->
			container << value
		}
	}

	private def buildGroovyExpression(condition) {
		if (condition == null) {
			return 'true'
		}
		def gCondition = condition
		gCondition = gCondition.replace("(", " ( ")
		gCondition = gCondition.replace(")", " ) ")
		gCondition = gCondition.replaceAll(/([^\w])and([^\w])/, '$1 && $2')
		gCondition = gCondition.replaceAll(/([^\w])or([^\w])/, '$1 || $2')
		gCondition = gCondition.replaceAll(/\s+/, " ")
		gCondition = " ${gCondition} "

		gCondition.findAll(/[ ][^ ]+?==[^ ]+?[ ]/).each { expression ->
			expression = expression.trim().replace("'", "\\'")
			def parts = expression.split('==')
			def left = parts[0]
			def right = parts[1]
			def gExpression = """String.valueOf(x.getValueFromContainerAt(y, '${left}', '[Undefined]')) == '${right}'"""

			gCondition = gCondition.replace(expression, gExpression)
		}
		gCondition.findAll(/[ ][^ ]+?=~[^ ]+?[ ]/).each { expression ->
			expression = expression.trim()
			def parts = expression.split('=~')
			def left = parts[0].replace("'", "\\'")
			def right = parts[1]
			def gExpression = """String.valueOf(x.getValueFromContainerAt(y, '${left}', '[Undefined]')) ==~ /${right}/"""

			gCondition = gCondition.replace(expression, gExpression)
		}
		return gCondition
	}

	private def startTraversal(container, condition, traversalHandler) {
		def gCondition = this.buildGroovyExpression(condition)

		traversalHandler.beforeTraversal?.call()
		this.doTraversal(0, [], container, gCondition, traversalHandler)
		traversalHandler.afterTraversal?.call()
	}

	private def elementMatchesCondtion(element, gCondition) {
		return Eval.xy(this, element, gCondition)
	}

	protected def doTraversal(level, accessors, element, condition, traversalHandler) {
		def conditionMatched = this.elementMatchesCondtion(element, condition)
		condition = conditionMatched ? 'true' : condition
		def path = "/" + accessors.join("/")
		path = path == "/" ? "${path}." : path

		if (element instanceof Map) {
			if (conditionMatched) {
				traversalHandler.beforeMapTraversal?.call(level, path, element)
			}
			element.each { key, value ->
				accessors << key
				this.doTraversal(level + 1, accessors, value, condition, traversalHandler)
				accessors.removeLast()
			}
			if (conditionMatched) {
				traversalHandler.afterMapTraversal?.call(level, path, element)
			}
		}
		else if (element instanceof List) {
			if (conditionMatched) {
				traversalHandler.beforeListTraversal?.call(level, path, element)
			}
			element.eachWithIndex { value, i ->
				accessors << i + 1
				this.doTraversal(level + 1, accessors, value, condition, traversalHandler)
				accessors.removeLast()
			}
			if (conditionMatched) {
				traversalHandler.afterListTraversal?.call(level, path, element)
			}
		}
		else {
			if (conditionMatched) {
				traversalHandler.onElementTraversal?.call(level, path, element)
			}
		}
	}

	def reset(resetStructure = true) {
		if (resetStructure) {
			this.structure = null
			this.aliasIndex	= [:]
		}
		this.elements = []

		return this
	}

	def addEntries(path, Object[] values) {
		def container = this.getElementAt(path)

		if (container instanceof Map) {
			this.addValuesAsMapContainerEntries(values, container)
		}
		else if (container instanceof List) {
			this.addValuesAsListContainerEntries(values, container)
		}

		return this
	}

	def addEntries(path, Map map) {
		def keysAndValues = []

		map.each { key, value ->
			keysAndValues << key
			keysAndValues << value
		}
		this.addEntries(path, keysAndValues as Object[])

		return this
	}

	def addEntries(path, List list) {
		this.addEntries(path, list as Object[])

		return this
	}

	def aliasExists(alias) {
		return alias in this.aliasIndex
	}

	def getValueAt(path) {
		def container = getElementAt(path, false)

		return container
	}

	def getValueAt(path, defaultValue) {
		def container

		try {
			container = getElementAt(path, false)
		}
		catch (Exception e) {
			container = defaultValue
		}
		return container
	}

	def getValueFromContainerAt(container, path, defaultValue) {
		try {
			container = getElementFromContainerAt(container, path, false)
		}
		catch (Exception e) {
			container = defaultValue
		}
		return container
	}

	def traverseWhere(condition, traversalHandler = DEFAULT_TRAVERSAL_HANDLER) {
		this.startTraversal(this.structure, condition, traversalHandler)

		return this
	}

	def traverseFromContainerWhere(container, condition, traversalHandler = DEFAULT_TRAVERSAL_HANDLER) {
		this.startTraversal(container, condition, traversalHandler)

		return this
	}

	def traverseFromPathWhere(path, condition, traversalHandler = DEFAULT_TRAVERSAL_HANDLER) {
		def container = getElementAt(path, false)

		if (container != null) {
			this.startTraversal(container, condition, traversalHandler)
		}
		return this
	}

	def traverse(traversalHandler = DEFAULT_TRAVERSAL_HANDLER) {
		return this.traverseWhere(null)
	}

	def traverseFromContainer(container, traversalHandler = DEFAULT_TRAVERSAL_HANDLER) {
		return this.traverseFromContainerWhere(container, null)
	}

	def traverseFromPath(path, traversalHandler = DEFAULT_TRAVERSAL_HANDLER) {
		return this.traverseFromPathWhere(path, null)
	}

	def indexWhere(condition) {
		return this.traverseWhere(condition, INDEX_TRAVERSAL_HANDLER)
	}

	def indexFromContainerWhere(container, condition) {
		return this.traverseFromContainerWhere(container, condition, INDEX_TRAVERSAL_HANDLER)
	}

	def indexFromPathWhere(path, condition) {
		return this.traverseFromPathWhere(path, condition, INDEX_TRAVERSAL_HANDLER)
	}

	def index() {
		return this.indexWhere(null)
	}

	def indexFromContainer(container) {
		return this.indexFromContainerWhere(container, null)
	}

	def indexFromPath(path) {
		return this.indexFromPathWhere(path, null)
	}

	def debug(title) {
		if (title != null) {
			def n = 120

			println "#" * n 
			println "${' ' * ((n - title.size()) / 2)}${title}"
			println "#" * n
			println ""
		}
		println this

		return this
	}

	String toString() {
		def s = ""
		s += "=========\n"
		s += "STRUCTURE\n"
		s += "=========\n"
		s += this.yaml.dump(this.structure).trim()
		s += "\n"
		s += "===========\n"
		s += "ALIAS INDEX\n"
		s += "===========\n"
		s += this.yaml.dump(this.aliasIndex).trim()
		s += "\n"
		
		return s
	}
}
