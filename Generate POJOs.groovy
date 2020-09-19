import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

config = [
        impSerializable  : true,
        extendBaseEntity : false,
        extendBaseService: true,
        useLombok        : false, // 使用注解，不生成get、set方法
        ModelNotNULL     : true, // 空值不返回注解
        ModelDate        : true, // 日期类格式注解
        baseMethods      : true, // ServiceImpl生成基础方法

        generateItem     : [
                "Entity",
                "Service",
                "ServiceImpl",
                "Repository",
                "RepositoryCustom",
                "RepositoryImpl",
        ]
]


baseEntityProperties = ["id", "createDate", "lastModifiedDate", "version"]

typeMapping = [
        (~/(?i)bool|boolean|tinyint/)     : "Boolean",
        (~/(?i)bigint/)                   : "Long",
        (~/int/)                          : "Integer",
        (~/(?i)float|double/)             : "Double",
        (~/(?i)decimal|real/)             : "java.math.BigDecimal",
        (~/(?i)datetime|timestamp/)       : "java.util.Date",
        (~/(?i)date/)                     : "java.sql.Date",
        (~/(?i)time/)                     : "java.sql.Time",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter {
        it instanceof DasTable && it.getKind() == ObjectKind.TABLE
    }.each {
        generate(it, dir)
    }
}

// 生成对应的文件
def generate(table, dir) {

    def entityPath = "${dir.toString()}\\entity",
        servicePath = "${dir.toString()}\\service",
        serviceImplPath = "${dir.toString()}\\service\\impl",
        repPath = "${dir.toString()}\\repository",
        repImpPath = "${dir.toString()}\\repository\\impl",
        controllerPath = "${dir.toString()}\\controller"

    mkdirs([entityPath, servicePath, serviceImplPath, repPath, repImpPath, controllerPath])

    System.out.println(table.getName())
    def entityName = javaName(table.getName(), true)
    def fields = calcFields(table)
    def basePackage = clacBasePackage(dir)

    if (isGenerate("Entity")) {
        genUTF8File(entityPath, "${entityName}.java").withPrintWriter { out -> genEntity(out, table, entityName, fields, basePackage) }
    }
    if (isGenerate("Service")) {
        genUTF8File(servicePath, "${entityName}Service.java").withPrintWriter { out -> genService(out, table, entityName, fields, basePackage) }
    }
    if (isGenerate("ServiceImpl")) {
        genUTF8File(serviceImplPath, "${entityName}ServiceImpl.java").withPrintWriter { out -> genServiceImpl(out, table, entityName, fields, basePackage) }
    }
    if (isGenerate("Repository")) {
        genUTF8File(repPath, "${entityName}Repository.java").withPrintWriter { out -> genRepository(out, table, entityName, fields, basePackage) }
    }
    if (isGenerate("RepositoryCustom")) {
        genUTF8File(repPath, "${entityName}RepositoryCustom.java").withPrintWriter { out -> genRepositoryCustom(out, entityName, basePackage) }
    }
    if (isGenerate("RepositoryImpl")) {
        genUTF8File(repImpPath, "${entityName}RepositoryImpl.java").withPrintWriter { out -> genRepositoryImpl(out, table, entityName, fields, basePackage) }
    }

}

// 是否需要被生成
def isGenerate(itemName) {
    config.generateItem.contains(itemName)
}

// 指定文件编码方式，防止中文注释乱码
def genUTF8File(dir, fileName) {
    new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, fileName)), "utf-8"))
}

// 生成每个字段
def genProperty(out, field) {

    out.println ""
    out.println "\t/**"
    out.println "\t * ${field.comment}"
    out.println "\t * default value: ${field.default}"
    out.println "\t */"
    // 默认表的第一个字段为主键
    if (field.position == 1) {
        out.println "\t@Id"
    }
    // 日期类加上注解
    if (field.dataType == "datetime" || field.dataType == "timestamp") {
        if (config.ModelDate) {
            out.println "\t@JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")"
        }
    }
    // 枚举不需要长度
    out.println "\t@Column(name = \"${field.colum}\", nullable = ${!field.isNotNull}${field.dataType == "enum" ? "" : field.dataType == "date" || field.dataType == "datetime" || field.dataType == "timestamp" ? "" : ", length = $field.len"})"
    out.println "\tprivate ${field.type} ${field.name};"
}

// 生成get、get方法
def genGetSet(out, field) {

    // get
    out.println "\t"
    out.println "\tpublic ${field.type} get${field.name.substring(0, 1).toUpperCase()}${field.name.substring(1)}() {"
    out.println "\t\treturn this.${field.name};"
    out.println "\t}"

    // set
    out.println "\t"
    out.println "\tpublic void set${field.name.substring(0, 1).toUpperCase()}${field.name.substring(1)}(${field.type} ${field.name}) {"
    out.println "\t\tthis.${field.name} = ${field.name};"
    out.println "\t}"
}

// 生成实体类
def genEntity(out, table, entityName, fields, basePackage) {
    out.println "package ${basePackage}.entity;"
    out.println ""
    if (config.extendBaseEntity) {
        out.println "import $baseEntityPackage;"
    }
    if (config.useLombok) {
        out.println "import lombok.Data;"
        out.println ""
    }
    if (config.impSerializable) {
        out.println "import java.io.Serializable;"
        out.println ""
    }

    out.println "import javax.persistence.*;"
    if (config.ModelNotNULL) {
        out.println "import com.fasterxml.jackson.annotation.JsonInclude;"
    }
    if (config.ModelDate) {
        out.println "import com.fasterxml.jackson.annotation.JsonFormat;"
    }
    out.println ""
    out.println "/**"
    out.println " * @author XuanWem Chen"
    out.println " * @email sproutgod667@gmail.com"
    out.println " * @date ${new Date().toString()}"
    out.println " * ${table.getComment()}"
    out.println " */"
    if (config.useLombok) {
        out.println "@Data"
    }
    if (config.ModelNotNULL) {
        out.println "@JsonInclude(JsonInclude.Include.NON_NULL)"
    }
    out.println "@Entity"
    out.println "@Table(name = \"${table.getName()}\")"
    out.println "public class $entityName${config.extendBaseEntity ? " extends BaseEntity" : ""}${config.impSerializable ? " implements Serializable" : ""} {"

    if (config.extendBaseEntity) {
        fields = fields.findAll { it ->
            !baseEntityProperties.any { it1 -> it1 == it.name }
        }
    }

    fields.each() {
        genProperty(out, it)
    }

    if (!config.useLombok) {
        fields.each() {
            genGetSet(out, it)
        }
    }
    out.println "}"
}

// 生成Service
def genService(out, table, entityName, fields, basePackage) {
    out.println "package ${basePackage}.service;"
    out.println ""
    // if (config.extendBaseService) {
    //   out.println "import $baseServicePackage;"
    out.println "import ${basePackage}.entity.$entityName;"
//  }
    out.println ""
    out.println ""
    out.println "/**"
    out.println " * @author XuanWem Chen"
    out.println " * @email sproutgod667@gmail.com"
    out.println " * @date ${new Date().toString()}"
    out.println " */"
    out.println "public interface ${entityName}Service${config.extendBaseService ? " extends BaseJpaService<$entityName, ${fields[0].type}>" : ""}  {"
    out.println ""
    out.println "}"
}
// 生成ServiceImpl
def genServiceImpl(out, table, entityName, fields, basePackage) {
    out.println "package ${basePackage}.service.impl;"
    out.println ""
    out.println "import ${basePackage}.repository.${entityName}Repository;"
    out.println "import ${basePackage}.service.${entityName}Service;"
    out.println "import ${basePackage}.entity.$entityName;"
    if (config.baseMethods) {
        out.println "import java.util.List;"
        out.println "import org.springframework.transaction.annotation.Transactional;"
        out.println "import org.springframework.data.domain.*;"
        out.println "import java.util.Optional;"
    }
    out.println "import org.springframework.stereotype.Service;"
    out.println ""
    out.println "import javax.annotation.Resource;"
    out.println ""
    out.println "/**"
    out.println " * @author XuanWem Chen"
    out.println " * @email sproutgod667@gmail.com"
    out.println " * @date ${new Date().toString()}"
    out.println " */"
    out.println "@Service"
    //out.println "public class ${entityName}ServiceImpl  implements ${entityName}Service<$entityName, ${fields[0].type}>  {"
    out.println "public class ${entityName}ServiceImpl  implements ${entityName}Service  {"
    out.println ""
    out.println "\t@Resource"
    out.println "\tprivate ${entityName}Repository rep;"
    out.println ""
    if (config.baseMethods) {
        //基础方法 save
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public ${entityName} save(${entityName} obj) {"
        out.println "\t\t return rep.save(obj);"
        out.println "\t }"
        out.println ""
        //基础方法 saveList
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t @Transactional(rollbackFor = Exception.class)"
        out.println "\t public List<${entityName}> saveAll(Iterable<${entityName}> list) {"
        out.println "\t\t return rep.saveAll(list);"
        out.println "\t }"
        out.println ""
        //基础方法 getOne
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public ${entityName} getOne(${fields[0].type} id) {"
        out.println "\t\t return rep.getOne(id);"
        out.println "\t }"
        out.println ""
        //基础方法 findById
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public ${entityName} findById(${fields[0].type} id) {"
        out.println "\t\t Optional<${entityName}> obj = rep.findById(id);"
        out.println "\t\t return obj.orElse(null);"
        out.println "\t }"
        out.println ""
        //基础方法 deleteById
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public void deleteById(${fields[0].type} id) {"
        out.println "\t\t rep.deleteById(id);"
        out.println "\t }"
        out.println ""
        //基础方法 deleteAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t @Transactional(rollbackFor = Exception.class)"
        out.println "\t public void deleteAll(List<${entityName}> list) {"
        out.println "\t\t rep.deleteAll(list);"
        out.println "\t }"
        out.println ""
        //基础方法 delete
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public void delete(${entityName} obj) {"
        out.println "\t\t rep.delete(obj);"
        out.println "\t }"
        out.println ""
        //基础方法 existsById
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public boolean existsById(${fields[0].type} id) {"
        out.println "\t\t return rep.existsById(id);"
        out.println "\t }"
        out.println ""
        //基础方法 count
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public long count() {"
        out.println "\t\t return rep.count();"
        out.println "\t }"
        out.println ""
        //基础方法 findAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public List<${entityName}> findAll() {"
        out.println "\t\t return rep.findAll();"
        out.println "\t }"
        out.println ""
        //基础方法 findAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public List<${entityName}> findAll(${entityName} obj) {"
        out.println "\t\t List<${entityName}> list = rep.findAll(Example.of(obj));"
        out.println "\t\t return list.size()<1?null:list;"
        out.println "\t }"
        out.println ""
        //基础方法 findAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public List<${entityName}> findAll(Sort sort) {"
        out.println "\t\t return rep.findAll(sort);"
        out.println "\t }"
        out.println ""
        //基础方法 findAllById
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public List<${entityName}> findAllById(Iterable<${fields[0].type}> ids) {"
        out.println "\t\t return rep.findAllById(ids);"
        out.println "\t }"
        out.println ""
        //基础方法 findAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public List<${entityName}> findAll(Example<${entityName}> e) {"
        out.println "\t\t return rep.findAll(e);"
        out.println "\t }"
        out.println ""
        //基础方法 findAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public List<${entityName}> findAll(Example<${entityName}> e, Sort sort) {"
        out.println "\t\t return rep.findAll(e,sort);"
        out.println "\t }"
        out.println ""
        //基础方法 findAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public Page<${entityName}> findAll(Pageable page) {"
        out.println "\t\t return rep.findAll(page);"
        out.println "\t }"
        out.println ""
        //基础方法 findAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public Page<${entityName}> findAll(Example<${entityName}> e, Pageable page) {"
        out.println "\t\t return rep.findAll(e,page);"
        out.println "\t }"
        out.println ""
        //基础方法 findAll
        out.println "\t ${config.extendBaseService ? "@Override" : "" }"
        out.println "\t public Page<${entityName}> findAll(${entityName} obj, Pageable page) {"
        out.println "\t\t return rep.findAll(Example.of(obj),page);"
        out.println "\t }"
        out.println ""
    }
    out.println "}"
}

// 生成Repository
def genRepository(out, table, entityName, fields, basePackage) {
    out.println "package ${basePackage}.repository;"
    out.println ""
    out.println "import ${basePackage}.entity.$entityName;"
    out.println "import org.springframework.data.jpa.repository.JpaRepository;"
    out.println ""
    out.println "/**"
    out.println " * @author XuanWem Chen"
    out.println " * @email sproutgod667@gmail.com"
    out.println " * @date ${new Date().toString()}"
    out.println " */"
    out.println "public interface ${entityName}Repository extends JpaRepository<$entityName, ${fields[0].type}>, ${entityName}RepositoryCustom {"
    out.println ""
    out.println "}"
}

// 生成RepositoryCustom
def genRepositoryCustom(out, entityName, basePackage) {
    out.println "package ${basePackage}.repository;"
    out.println ""
    out.println "/**"
    out.println " * @author XuanWem Chen"
    out.println " * @email sproutgod667@gmail.com"
    out.println " * @date ${new Date().toString()}"
    out.println " */"
    out.println "public interface ${entityName}RepositoryCustom {"
    out.println ""
    out.println "}"
}

// 生成RepositoryImpl
def genRepositoryImpl(out, table, entityName, fields, basePackage) {
    out.println "package ${basePackage}.repository.impl;"
    out.println ""
    out.println "import ${basePackage}.repository.${entityName}RepositoryCustom;"
    out.println "import org.springframework.stereotype.Repository;"
    out.println ""
    out.println "import javax.persistence.EntityManager;"
    out.println "import javax.persistence.PersistenceContext;"
    out.println ""
    out.println "/**"
    out.println " * @author XuanWem Chen"
    out.println " * @email sproutgod667@gmail.com"
    out.println " * @date ${new Date().toString()}"
    out.println " */"
    out.println "@Repository"
    out.println "public class ${entityName}RepositoryImpl implements ${entityName}RepositoryCustom {"
    out.println ""
    out.println "\t@PersistenceContext"
    out.println "\tprivate EntityManager em;"
    out.println "}"
}

// 生成文件夹
def mkdirs(dirs) {
    dirs.forEach {
        def f = new File(it)
        if (!f.exists()) {
            f.mkdirs()
        }
    }
}

def clacBasePackage(dir) {
    dir.toString()
            .replaceAll("^.+\\\\src\\\\main\\\\java\\\\", "")
            .replaceAll("\\\\", ".")
}

def isBaseEntityProperty(property) {
    baseEntityProperties.find { it == property } != null
}

// 转换类型
def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->

        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name     : javaName(col.getName(), false),
                           colum    : col.getName(),
                           type     : typeStr,
                           dataType : col.getDataType().toString().replaceAll(/\(.*\)/, "").toLowerCase(),
                           len      : col.getDataType().toString().replaceAll(/[^\d]/, ""),
                           default  : col.getDefault(),
                           comment  : col.getComment(),
                           isNotNull: col.isNotNull(),
                           position : col.getPosition(),
                   ]]

    }
}

def javaName(str, capitalize) {
    def s = str.split(/(?<=[^\p{IsLetter}])/).collect { Case.LOWER.apply(it).capitalize() }
            .join("").replaceAll(/[^\p{javaJavaIdentifierPart}]/, "_").replaceAll(/_/, "")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}