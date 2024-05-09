"use strict";(self.webpackChunksmithy4s=self.webpackChunksmithy4s||[]).push([[7777],{3905:(e,t,i)=>{i.d(t,{Zo:()=>c,kt:()=>h});var n=i(7294);function r(e,t,i){return t in e?Object.defineProperty(e,t,{value:i,enumerable:!0,configurable:!0,writable:!0}):e[t]=i,e}function o(e,t){var i=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);t&&(n=n.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),i.push.apply(i,n)}return i}function a(e){for(var t=1;t<arguments.length;t++){var i=null!=arguments[t]?arguments[t]:{};t%2?o(Object(i),!0).forEach((function(t){r(e,t,i[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(i)):o(Object(i)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(i,t))}))}return e}function s(e,t){if(null==e)return{};var i,n,r=function(e,t){if(null==e)return{};var i,n,r={},o=Object.keys(e);for(n=0;n<o.length;n++)i=o[n],t.indexOf(i)>=0||(r[i]=e[i]);return r}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)i=o[n],t.indexOf(i)>=0||Object.prototype.propertyIsEnumerable.call(e,i)&&(r[i]=e[i])}return r}var l=n.createContext({}),u=function(e){var t=n.useContext(l),i=t;return e&&(i="function"==typeof e?e(t):a(a({},t),e)),i},c=function(e){var t=u(e.components);return n.createElement(l.Provider,{value:t},e.children)},d="mdxType",m={inlineCode:"code",wrapper:function(e){var t=e.children;return n.createElement(n.Fragment,{},t)}},p=n.forwardRef((function(e,t){var i=e.components,r=e.mdxType,o=e.originalType,l=e.parentName,c=s(e,["components","mdxType","originalType","parentName"]),d=u(i),p=r,h=d["".concat(l,".").concat(p)]||d[p]||m[p]||o;return i?n.createElement(h,a(a({ref:t},c),{},{components:i})):n.createElement(h,a({ref:t},c))}));function h(e,t){var i=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var o=i.length,a=new Array(o);a[0]=p;var s={};for(var l in t)hasOwnProperty.call(t,l)&&(s[l]=t[l]);s.originalType=e,s[d]="string"==typeof e?e:r,a[1]=s;for(var u=2;u<o;u++)a[u]=i[u];return n.createElement.apply(null,a)}return n.createElement.apply(null,i)}p.displayName="MDXCreateElement"},2248:(e,t,i)=>{i.r(t),i.d(t,{assets:()=>l,contentTitle:()=>a,default:()=>m,frontMatter:()=>o,metadata:()=>s,toc:()=>u});var n=i(7462),r=(i(7294),i(3905));const o={sidebar_label:"Smithy build config",title:"Smithy Build Configuration"},a=void 0,s={unversionedId:"guides/smithy-build-config",id:"guides/smithy-build-config",title:"Smithy Build Configuration",description:"Introduction",source:"@site/../docs/target/jvm-2.13/mdoc/06-guides/smithy-build-config.md",sourceDirName:"06-guides",slug:"/guides/smithy-build-config",permalink:"/smithy4s/docs/guides/smithy-build-config",draft:!1,editUrl:"https://github.com/disneystreaming/smithy4s/edit/main/modules/docs/src/06-guides/smithy-build-config.md",tags:[],version:"current",frontMatter:{sidebar_label:"Smithy build config",title:"Smithy Build Configuration"},sidebar:"tutorialSidebar",previous:{title:"Smithy4s to Smithy",permalink:"/smithy4s/docs/guides/schema-to-smithy"},next:{title:"Smithy4s Transformations",permalink:"/smithy4s/docs/guides/smithy4s-transformations"}},l={},u=[{value:"Introduction",id:"introduction",level:2},{value:"Customizing OpenAPI generation via smithy build",id:"customizing-openapi-generation-via-smithy-build",level:3}],c={toc:u},d="wrapper";function m(e){let{components:t,...i}=e;return(0,r.kt)(d,(0,n.Z)({},c,i,{components:t,mdxType:"MDXLayout"}),(0,r.kt)("h2",{id:"introduction"},"Introduction"),(0,r.kt)("p",null,"Smithy provides the ability to configure the Smithy build and output by a ",(0,r.kt)("a",{parentName:"p",href:"https://smithy.io/2.0/guides/smithy-build-json.html#smithy-build-json"},"smithy-build configuration file"),". As smithy4s uses its own build logic, it generally loads its configuration from elsewhere. However, limited support for build customization using a Smithy build configuration file is available. In particular, the ",(0,r.kt)("a",{parentName:"p",href:"https://smithy.io/2.0/guides/model-translations/converting-to-openapi.html"},"OpenAPI plugin")," can be used to customize the OpenAPI generation."),(0,r.kt)("h3",{id:"customizing-openapi-generation-via-smithy-build"},"Customizing OpenAPI generation via smithy build"),(0,r.kt)("p",null,"In order to apply a custom OpenAPI config, you need a ",(0,r.kt)("inlineCode",{parentName:"p"},"smithy-build.json")," file with the OpenAPI configuration, such as the following:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-json"},'{\n  "version": "1.0",\n  "plugins": {\n    "openapi": {\n      "service": "smithy.example#Weather",\n      "version": "3.1.0",\n      "jsonAdd": {\n        "/info/title": "Replaced title value",\n        "/info/nested/foo": {\n          "hi": "Adding this object created intermediate objects too!"\n        },\n        "/info/nested/foo/baz": true\n      }\n    }\n  }\n}\n')),(0,r.kt)("p",null,"This file can then used to configure codegen via the appropriate SBT setting:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'Compile / smithyBuild := Some(baseDirectory.value / "smithy-build.json")\n')),(0,r.kt)("p",null,"It can also be configured in Mill:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-scala"},'override def smithyBuild = Some(PathRef(millSourcePath / "smithy-build.json"))\n')),(0,r.kt)("p",null,"Or, if you are using codegen directly via the command line tool, it can be passed via the argument ",(0,r.kt)("inlineCode",{parentName:"p"},"--smithy-build ./smithy-build.json"),"."),(0,r.kt)("p",null,"The generated OpenAPI should then have the configured transformations applied."))}m.isMDXComponent=!0}}]);