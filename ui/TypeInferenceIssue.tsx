export interface TextModalProps {
  title: string;
  body: string;
  buttonText: string;
}

const TextModal = (props: TextModalProps) => {
  const {title, body, buttonText} = props;
  return `${title} ${body} ${buttonText}`;
};
const textModalResult = TextModal({title: 'hello', body: 'world' , buttonText: 'A button'});
console.log(textModalResult);


interface ErrorModalWrapperInterface {
  title: string;
  body: string | number[] | {foo: string};
  buttonText: string;
}
class ErrorModalWrapper implements ErrorModalWrapperInterface {
  title;
  body;
  buttonText;

  constructor(props: ErrorModalWrapperInterface) {
    const {title, body, buttonText} = props;
    this.title = title;
    this.body = body;
    this.buttonText = buttonText;
  }

  render() {
    const {title, body, buttonText} = this;
    return 'Wrapper: ' + TextModal({title, body, buttonText});
  }
}
const errorModalWrapperArray = new ErrorModalWrapper({title: 'Error Hello', body: [5, 3], buttonText: 'Error Button'});
console.log(errorModalWrapperArray.render());

const errorModalWrapperObject = new ErrorModalWrapper({title: 'Object Hello', body: {foo: 'bar'}, buttonText: 'Object Button'});
console.log(errorModalWrapperObject.render());


// const indirection = (props: ErrorModalWrapperInterface) => {
//   const {title, body, buttonText} = props;
//   return 'Indirection: ' + TextModal({title, body, buttonText});
// };
// const indirectionResult = indirection({title: 'Indirection Hello', body: 'Body Hello', buttonText: 'Indirection Button'});
// console.log(indirectionResult);



