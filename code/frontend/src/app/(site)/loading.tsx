import PreLoader from "@/components/Common/PreLoader";

type LoadingProps = {
  children?: React.ReactNode;
};

function Loading({ children }: LoadingProps) {
  return (
    <>
      <PreLoader />
    </>
  );
}

export default Loading;
