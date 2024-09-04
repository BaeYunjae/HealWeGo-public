## Content

- [Type](#type)
- [Branch](#branch)
  - [Template](#template)
  - [설명](#설명)
- [Commit message](#commit-message)
  - [Template](#template-1)
  - [설명](#설명-1)

<br/><br/>

# Type

    아래 내용을 참고해서 복붙한다.(대소문자 주의!)
    
    branch - 첫 글자 대문자 Feat
    commit message - 첫 글자 소문자 feat

|*제목*|*설명*|
|---|---|
|feat|새로운 기능 추가|
|fix|버그 수정|
|docs|문서 수정|
|style|코드 포맷팅, 세미콜론 누락 등 코드 자체의 변경이 없는 경우|
|refactor|코드 리팩토링|
|test|테스트 코드|
|chore|패키지 매니저 수정, 그 외 기타 수정(.gitignore 등)|
|design|CSS 등 사용자 UI 디자인 변경|
|comment|필요한 주석 추가 및 변경|
|rename|파일 또는 폴더 명을 수정하거나 옮기는 작업만 한 경우|
|remove|파일을 삭제하는 작업만 수행한 경우|
|!HOTFIX|급하게 치명적인 버그를 고쳐야 하는 경우|

<br/><br/>

# Branch

## Template

[부모 브랜치/Type] 기능

        ex) [android/Feat] 로그인
		    [morai/Fix] 경로 추정

## 설명
1. Type
	
	* Type convention 참고

2. 기능

	* 기능 명세서의 기능명에 따름

<br/><br/>

# Commit message

## Template

        type: 소기능

        ex) feat: 로그인 버튼 구현
		test: DB 업데이트 테스트 코드 작성

## 설명
1. Type
	
	* Type convention 참고

2. 소기능

   * 해당 브랜치의 기능에 맞는 소기능을 간결하게 작성
   * 소기능은 최대 50글자를 넘지 않도록 하고, 마침표 및 특수기호는 사용하지 않는다.
   * 개조식 구문으로 작성한다. 완전한 서술형 문장이 아니라, 명사 위주로 간결하게 서술한다.  
    예) 이메일 기능 구현(O), 이메일 기능 구현했습니다.(X)